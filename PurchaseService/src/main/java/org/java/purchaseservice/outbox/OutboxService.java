package org.java.purchaseservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

	private final OutboxRepository repo;
	private final ObjectMapper mapper = new ObjectMapper();

	/**
	 * Serialize JSON and write to Outbox
	 */
	public String saveEvent(String eventType, Object payloadObj, String aggregateId) {
		try {
			if (eventType == null || eventType.isBlank()) {
				throw new IllegalArgumentException("eventType must not be blank");
			}
			if (payloadObj == null) {
				throw new IllegalArgumentException("payload must not be null");
			}

			String payload;
			if (payloadObj instanceof String) {
				payload = (String) payloadObj;
			} else {
				payload = mapper.writeValueAsString(payloadObj);
			}

			String outboxId = repo.save(eventType, payload, aggregateId);
			log.info("【Outbox】Event saved successfully. outboxId={}, eventType={}, aggregateId={}, payloadLength={}",
					outboxId, eventType, aggregateId, payload.length());
			log.debug("【Outbox】Event payload: {}", payload);

			return outboxId; // return outbox id
		} catch (Exception e) {
			log.error("【Outbox】Failed to serialize payload for eventType={}, aggregateId={}",
					eventType, aggregateId, e);
			throw new IllegalArgumentException("Serialize payload failed", e);
		}
	}
}