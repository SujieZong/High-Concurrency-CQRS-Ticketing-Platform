package org.java.purchaseservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.purchaseservice.publisher.MessagePublisher;
import org.java.purchaseservice.service.dlq.DeadLetterQueueService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TicketEventListener {
	private final MessagePublisher messagePublisher;
	private final ObjectMapper objectMapper;
	private final DeadLetterQueueService deadLetterQueueService;

	/**
	 * Listen to TicketCreatedEvent and send to Kafka
	 * Changed from @TransactionalEventListener to @EventListener because:
	 * 1. No actual DB transaction in purchaseTicket()
	 * 2. Kafka message itself serves as event source
	 */
	@EventListener
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
				deadLetterQueueService.sendToDeadLetterQueue(payload, partitionKey, "Kafka kafkaPublish returned false");
			}

		} catch (Exception e) {
			log.error("【EventListener】Error processing TicketCreatedEvent: ticketId={}", ticketCreatedEvent.getTicketId(), e);

			if (payload != null) {
				deadLetterQueueService.sendToDeadLetterQueue(payload, partitionKey, "Exception: " + e.getMessage());
			}
		}

	}
}
