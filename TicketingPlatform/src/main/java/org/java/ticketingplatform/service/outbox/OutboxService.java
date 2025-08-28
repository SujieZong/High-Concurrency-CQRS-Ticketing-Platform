package org.java.ticketingplatform.service.outbox;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.java.ticketingplatform.domain.OutboxEvent;
import org.java.ticketingplatform.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

	private final OutboxEventRepository repo;

	@Transactional
	public void saveEvent(String eventType, String payload) {
		OutboxEvent e = new OutboxEvent();
		e.setEventType(eventType);
		e.setPayload(payload);
		repo.save(e);
	}
}
