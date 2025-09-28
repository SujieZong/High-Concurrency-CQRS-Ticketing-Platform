package org.java.purchaseservice;

import org.java.purchaseservice.outbox.OutboxRepository;
import org.java.purchaseservice.outbox.OutboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

// AssertJ
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Mockito
import static org.mockito.Mockito.*;

class OutboxServiceTest {

	@Test
	void saveEvent_withStringPayload_passThroughAndReturnOutboxId() {
		OutboxRepository repo = mock(OutboxRepository.class);
		OutboxService svc = new OutboxService(repo);

		when(repo.save(eq("ticket.created"), eq("{\"k\":\"v\"}"), eq("agg-1")))
				.thenReturn("outbox-123");

		String id = svc.saveEvent("ticket.created", "{\"k\":\"v\"}", "agg-1");

		assertThat(id).isEqualTo("outbox-123");
		verify(repo, times(1)).save(eq("ticket.created"), eq("{\"k\":\"v\"}"), eq("agg-1"));
		verifyNoMoreInteractions(repo);
	}

	@Test
	void saveEvent_withMapPayload_serializesToJsonAndCallsRepo() {
		OutboxRepository repo = mock(OutboxRepository.class);
		OutboxService svc = new OutboxService(repo);

		when(repo.save(anyString(), anyString(), anyString())).thenReturn("outbox-456");

		Map<String, Object> payloadObj = Map.of("ticketId", "T-1", "zoneId", 3);

		String id = svc.saveEvent("ticket.created", payloadObj, "agg-2");

		assertThat(id).isEqualTo("outbox-456");

		ArgumentCaptor<String> payloadCap = ArgumentCaptor.forClass(String.class);
		verify(repo).save(eq("ticket.created"), payloadCap.capture(), eq("agg-2"));
		String json = payloadCap.getValue();

		assertThat(json).contains("\"ticketId\":\"T-1\"");
		assertThat(json).contains("\"zoneId\":3");
	}

	@Test
	void saveEvent_whenSerializationFails_throwIllegalArgumentException() {
		class SelfRef { @SuppressWarnings("unused") SelfRef me = this; }

		OutboxRepository repo = mock(OutboxRepository.class);
		OutboxService svc = new OutboxService(repo);

		assertThatThrownBy(() -> svc.saveEvent("ticket.created", new SelfRef(), "agg-3"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Serialize payload failed");

		verifyNoInteractions(repo);
	}

	@Test
	void saveEvent_withNullPayload_throwsException() {
		OutboxRepository repo = mock(OutboxRepository.class);
		OutboxService svc = new OutboxService(repo);

		assertThatThrownBy(() -> svc.saveEvent("ticket.created", null, "agg-9"))
				.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(repo);
	}

	static class Ticket {
		String ticketId = "T-100";
		int zoneId = 2;
	}


	@Test
	void saveEvent_withNullAggregateId_stillSaves() {
		OutboxRepository repo = mock(OutboxRepository.class);
		OutboxService svc = new OutboxService(repo);

		when(repo.save(anyString(), anyString(), isNull())).thenReturn("outbox-789");

		String id = svc.saveEvent("ticket.created", "{\"k\":\"v\"}", null);

		assertThat(id).isEqualTo("outbox-789");
	}
}