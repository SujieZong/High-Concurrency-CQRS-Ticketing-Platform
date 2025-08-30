package org.java.ticketingplatform;

import org.java.purchaseservice.domain.OutboxEvent;
import org.java.purchaseservice.repository.OutboxEventRepository;
import org.java.purchaseservice.service.outbox.OutboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

	@Test
	void saveEvent_persistsEntityWithDefaultFlags() {
		OutboxEventRepository repo = mock(OutboxEventRepository.class);
		OutboxService svc = new OutboxService(repo);

		svc.saveEvent("ticket.created", "{\"k\":\"v\"}");

		ArgumentCaptor<OutboxEvent> cap = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(repo, times(1)).save(cap.capture());

		OutboxEvent e = cap.getValue();
		assertThat(e.getEventType()).isEqualTo("ticket.created");
		assertThat(e.getPayload()).isEqualTo("{\"k\":\"v\"}");
		assertThat(e.isSent()).isFalse();
	}
}
