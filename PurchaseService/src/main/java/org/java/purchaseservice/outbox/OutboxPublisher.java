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

	@Scheduled(fixedDelay = 500) //every 0.5 second scan
	public void flush() {
		int pageSize = 100;

		for (Page<OutboxEvent> page : repo.queryUnsent(pageSize)) {
			for (OutboxEvent e : page.items()) {
				if (e.getNextAttemptAt() != null && e.getNextAttemptAt().isAfter(Instant.now())) {
					continue;
				}

				// identify if the redo process should keep going
				int attempts = (e.getAttempts() == null ? 0 : e.getAttempts());
				if (attempts >= MAX_ATTEMPTS) {
					repo.markDead(e.getId(), Instant.now()); //stop the process
					log.error("Outbox event marked DEAD after max attempts. outboxId={}, attempts={}, eventType={}, aggregateId={}",
							e.getId(), attempts, e.getEventType(), e.getAggregateId());
					continue; //log the data as error message log
				}

				try {
					String routingOrPartitionKey = extractKey(e); // Rabbit: routingKey；未来 Kafka: partitionKey
					boolean ok = messagePublisher.kafkaPublish(e.getPayload(), routingOrPartitionKey); //改成了Kafka
					if (!ok) throw new IllegalStateException("rabbitPublish failed");

					boolean marked = repo.markSent(e.getId(), Instant.now());
					if (!marked) {
						log.info("Already marked by another worker, id={}", e.getId());
					} else {
						log.info("Outbox sent successfully. outboxId={}", e.getId());
					}
				} catch (Exception ex) {
					repo.recordRetry(e.getId(),
							n -> Instant.now().plusSeconds(Math.min(60, (long) Math.pow(2, n)))
					);
					log.warn("Outbox rabbitPublish failed. outboxId={}, attempts={}, err={}",
							e.getId(), attempts + 1, ex.toString());
				}
			}
		}
	}


	private String extractKey(OutboxEvent e) {
		if (e.getAggregateId() != null && !e.getAggregateId().isBlank()) return e.getAggregateId();
		try {
			var node = mapper.readTree(e.getPayload());
			if (node.hasNonNull("ticketId")) return node.get("ticketId").asText();
			if (node.hasNonNull("id")) return node.get("id").asText();
		} catch (Exception ignore) {
		}
		return UUID.randomUUID().toString();
	}

}