package org.java.purchaseservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


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
			return repo.save(eventType, payload, aggregateId); //return outbox id
		} catch (Exception e) {
			throw new IllegalArgumentException("Serialize payload failed", e);
		}
	}
}