package org.java.purchaseservice.service.dlq;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.purchaseservice.event.TicketCreatedEvent;
import org.java.purchaseservice.service.redis.SeatOccupiedRedisFacade;
import org.springframework.stereotype.Service;

/**
 * Dead Letter Queue handler with compensating transaction. When Kafka publishing fails, releases
 * the Redis seat to prevent inventory leaks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaDeadLetterQueueService implements DeadLetterQueueService {

  private final ObjectMapper objectMapper;
  private final SeatOccupiedRedisFacade seatOccupiedRedisFacade;

  @Override
  public void sendToDeadLetterQueue(String payload, String partitionKey, String errorReason) {
    try {
      // Parse the ORIGINAL event payload
      TicketCreatedEvent event = objectMapper.readValue(payload, TicketCreatedEvent.class);

      // Release the seat in Redis, prevents seat hold when Kafka publishing fails
      try {
        seatOccupiedRedisFacade.releaseSeat(
            event.getEventId(),
            event.getVenueId(),
            event.getZoneId(),
            event.getRow(),
            event.getColumn());
        log.warn(
            "【DLQ】Compensating transaction: Released seat for failed message ticketId={}, seat={}-{}",
            event.getTicketId(),
            event.getRow(),
            event.getColumn());
      } catch (Exception releaseEx) {
        log.error(
            "【DLQ】CRITICAL: Failed to release seat for ticketId={}, seat={}-{}",
            event.getTicketId(),
            event.getRow(),
            event.getColumn(),
            releaseEx);
      }

      // DLQ message with metadata
      Map<String, Object> dlqMessage = new HashMap<>();
      dlqMessage.put("originalPayload", payload);
      dlqMessage.put("partitionKey", partitionKey);
      dlqMessage.put("errorReason", errorReason);
      dlqMessage.put("timestamp", Instant.now().toString());
      dlqMessage.put("retryCount", 0);
      dlqMessage.put("retryable", true);
      dlqMessage.put("serviceName", "PurchaseService");
      dlqMessage.put("ticketId", event.getTicketId());
      dlqMessage.put("eventId", event.getEventId());
      dlqMessage.put("seat", event.getRow() + "-" + event.getColumn());

      // Serialize to JSON for logging/persistence
      String dlqPayload = objectMapper.writeValueAsString(dlqMessage);

      // Log the DLQ message (TODO: send to DLQ topic or database, currently only Logs)
      log.error("【DLQ】==================== DEAD LETTER QUEUE ====================");
      log.error("【DLQ】Ticket ID: {}", event.getTicketId());
      log.error("【DLQ】Event ID: {}", event.getEventId());
      log.error("【DLQ】Seat: {}", event.getRow() + "-" + event.getColumn());
      log.error("【DLQ】Partition Key: {}", partitionKey);
      log.error("【DLQ】Error Reason: {}", errorReason);
      log.error("【DLQ】Timestamp: {}", dlqMessage.get("timestamp"));
      log.error("【DLQ】Full DLQ Message: {}", dlqPayload);
      log.error("【DLQ】============================================================");

    } catch (Exception e) {
      log.error("【DLQ】CRITICAL: Failed to process DLQ message!");
      log.error("【DLQ】Partition Key: {}", partitionKey);
      log.error("【DLQ】Error Reason: {}", errorReason);
      log.error("【DLQ】Original Payload: {}", payload);
      log.error("【DLQ】Processing Error: {}", e.getMessage(), e);
    }
  }
}
