package org.java.purchaseservice.service.outbox;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.java.purchaseservice.domain.OutboxEvent;
import org.java.purchaseservice.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {
// save traction to outbox

	private final OutboxEventRepository repo;

	@Transactional
	public void saveEvent(String eventType, String payload) {
		OutboxEvent e = new OutboxEvent();
		e.setEventType(eventType);
		e.setPayload(payload);
		repo.save(e);
	}
}
