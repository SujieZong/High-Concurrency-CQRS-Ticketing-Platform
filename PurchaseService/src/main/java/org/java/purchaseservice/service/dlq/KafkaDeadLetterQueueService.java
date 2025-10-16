package org.java.purchaseservice.service.dlq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO: store the dead letter somewhere, currently in as logs.
 * Could be DLQ Topic or Kafka persistent to disk/volumn
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaDeadLetterQueueService implements DeadLetterQueueService {

	private final ObjectMapper objectMapper;

	@Override
	public void sendToDeadLetterQueue(String payload, String partitionKey, String errorReason) {
		try {

			Map<String, Object> dlqMessage = new HashMap<>();
			dlqMessage.put("originalPayload", payload);
			dlqMessage.put("partitionKey", partitionKey);
			dlqMessage.put("errorReason", errorReason);
			dlqMessage.put("timestamp", Instant.now().toString());
			dlqMessage.put("retryCount", 0);
			dlqMessage.put("retryable", true);
			dlqMessage.put("serviceName", "PurchaseService");

			// serialize to JSON
			String dlqPayload = objectMapper.writeValueAsString(dlqMessage);

			//Save the error as log
			log.error("【DLQ】==================== DEAD LETTER QUEUE ====================");
			log.error("【DLQ】Partition Key: {}", partitionKey);
			log.error("【DLQ】Error Reason: {}", errorReason);
			log.error("【DLQ】Timestamp: {}", dlqMessage.get("timestamp"));
			log.error("【DLQ】Full DLQ Message: {}", dlqPayload);
			log.error("【DLQ】============================================================");

		} catch (Exception e) {
			log.error("【DLQ】CRITICAL: Failed to serialize DLQ message!");
			log.error("【DLQ】Partition Key: {}", partitionKey);
			log.error("【DLQ】Error Reason: {}", errorReason);
			log.error("【DLQ】Original Payload: {}", payload);
			log.error("【DLQ】Serialization Error: {}", e.getMessage(), e);
		}
	}
}
