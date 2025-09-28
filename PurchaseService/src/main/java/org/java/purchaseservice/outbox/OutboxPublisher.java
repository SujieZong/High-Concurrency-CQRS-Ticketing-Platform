package org.java.purchaseservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

	private final OutboxRepository repo;
	private final OutboxMessagePublisher messagePublisher;
	private final ObjectMapper mapper = new ObjectMapper();
	private static final int MAX_ATTEMPTS = 10;

	@Scheduled(fixedDelay = 500) // every 0.5 second scan
	public void flush() {
		int pageSize = 100;
		int processedCount = 0;
		int skippedCount = 0;
		int failedCount = 0;
		int successCount = 0;

		for (Page<OutboxEvent> page : repo.queryUnsent(pageSize)) {
			for (OutboxEvent e : page.items()) {
				processedCount++;

				if (e.getNextAttemptAt() != null && e.getNextAttemptAt().isAfter(Instant.now())) {
					skippedCount++;
					log.debug("【OutboxPublisher】Skipping event due to next attempt time. outboxId={}, nextAttemptAt={}",
							e.getId(), e.getNextAttemptAt());
					continue;
				}

				// identify if the redo process should keep going
				int attempts = (e.getAttempts() == null ? 0 : e.getAttempts());
				if (attempts >= MAX_ATTEMPTS) {
					repo.markDead(e.getId(), Instant.now()); // stop the process
					log.error(
							"【OutboxPublisher】Event marked DEAD after max attempts. outboxId={}, attempts={}, eventType={}, aggregateId={}",
							e.getId(), attempts, e.getEventType(), e.getAggregateId());
					failedCount++;
					continue; // log the data as error message log
				}

				try {
					String partitionKey = extractKey(e); // Kafka: partitionKey
					log.info(
							"【OutboxPublisher】Publishing event to Kafka. outboxId={}, eventType={}, partitionKey={}, attempts={}",
							e.getId(), e.getEventType(), partitionKey, attempts);

					boolean ok = messagePublisher.kafkaPublish(e.getPayload(), partitionKey); // 改成了Kafka
					if (!ok)
						throw new IllegalStateException("kafkaPublish failed");

					boolean marked = repo.markSent(e.getId(), Instant.now());
					if (!marked) {
						log.info("【OutboxPublisher】Already marked by another worker, id={}", e.getId());
					} else {
						log.info("【OutboxPublisher】Event sent successfully. outboxId={}", e.getId());
						successCount++;
					}
				} catch (Exception ex) {
					repo.recordRetry(e.getId(),
							n -> Instant.now().plusSeconds(Math.min(60, (long) Math.pow(2, n))));
					log.warn("【OutboxPublisher】kafkaPublish failed. outboxId={}, attempts={}, err={}",
							e.getId(), attempts + 1, ex.toString());
					failedCount++;
				}
			}
		}

		if (processedCount > 0) {
			log.info("【OutboxPublisher】Flush completed. processed={}, success={}, failed={}, skipped={}",
					processedCount, successCount, failedCount, skippedCount);
		}
	}

	private String extractKey(OutboxEvent e) {
		if (e.getAggregateId() != null && !e.getAggregateId().isBlank())
			return e.getAggregateId();
		try {
			var node = mapper.readTree(e.getPayload());
			if (node.hasNonNull("ticketId"))
				return node.get("ticketId").asText();
			if (node.hasNonNull("id"))
				return node.get("id").asText();
		} catch (Exception ignore) {
		}
		return UUID.randomUUID().toString();
	}

}