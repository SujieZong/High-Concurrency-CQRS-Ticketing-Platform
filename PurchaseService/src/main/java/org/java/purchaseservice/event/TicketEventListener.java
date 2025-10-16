package org.java.purchaseservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.purchaseservice.publisher.MessagePublisher;
import org.java.purchaseservice.service.dlq.DeadLetterQueueService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class TicketEventListener {
	private final MessagePublisher messagePublisher;
	private final ObjectMapper objectMapper;
	private final DeadLetterQueueService deadLetterQueueService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleTicketCreation(TicketCreatedEvent ticketCreatedEvent) {
		String payload = null;
		String partitionKey = ticketCreatedEvent.getPartitionKey();

		try {
			log.info("【EventListener】Processing TicketCreatedEvent: ticketId={}", ticketCreatedEvent.getTicketId());

			payload = objectMapper.writeValueAsString(ticketCreatedEvent);

			boolean sent = messagePublisher.kafkaPublish(payload, partitionKey);

			if (sent) {
				log.info("【EventListener】Event sent to Kafka successfully: ticketId={}", ticketCreatedEvent.getTicketId());
			} else {
				log.error("【EventListener】Failed to send event to Kafka: ticketId={}", ticketCreatedEvent.getTicketId());
				deadLetterQueueService.sendToDeadLetterQueue(
					payload,
					partitionKey,
					"Kafka kafkaPublish returned false"
				);
			}

		} catch (Exception e) {
			log.error("【EventListener】Error processing TicketCreatedEvent: ticketId={}", ticketCreatedEvent.getTicketId(), e);

			// 序列化失败或其他异常，也发送到死信队列
			if (payload != null) {
				deadLetterQueueService.sendToDeadLetterQueue(
					payload,
					partitionKey,
					"Exception: " + e.getMessage()
				);
			}
		}

	}
}
