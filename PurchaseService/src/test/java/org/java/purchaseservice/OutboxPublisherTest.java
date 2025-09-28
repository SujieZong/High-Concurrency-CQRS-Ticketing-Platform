package org.java.purchaseservice;

import org.java.purchaseservice.outbox.OutboxEvent;
import org.java.purchaseservice.outbox.OutboxMessagePublisher;
import org.java.purchaseservice.outbox.OutboxPublisher;
import org.java.purchaseservice.outbox.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

	@Mock
	OutboxRepository repo;

	@Mock
	OutboxMessagePublisher messagePublisher;

	@Captor
	ArgumentCaptor<String> idCaptor;

	@Captor
	ArgumentCaptor<String> keyCaptor;

	OutboxPublisher publisher;

	// ---------- Outbox info to PageIterable ----------
	private static PageIterable<OutboxEvent> pagesOf(List<OutboxEvent> items) {
		SdkIterable<Page<OutboxEvent>> sdkIt =
				() -> Collections.singletonList(Page.builder(OutboxEvent.class).items(items).build()).iterator();
		return PageIterable.create(sdkIt);
	}

	private static OutboxEvent evt(
			String id, String aggregateId, String payload, Integer attempts, Integer sent, Instant nextAttemptAt) {

		OutboxEvent e = new OutboxEvent();
		e.setId(id);
		e.setAggregateId(aggregateId);
		e.setPayload(payload);
		e.setAttempts(attempts);
		e.setSent(sent);
		e.setNextAttemptAt(nextAttemptAt);
		e.setCreatedAt(Instant.now());
		e.setUpdatedAt(Instant.now());
		e.setEventType("ticket.created");
		return e;
	}

	@BeforeEach
	void setUp() {
		publisher = new OutboxPublisher(repo, messagePublisher);
	}

	@Test
	void flush_success_sendsAndMarksSent() {
		// given
		OutboxEvent e = evt("id-1", "agg-1", "{\"ticketId\":\"t-1\"}", 0, 0, null);
		when(repo.queryUnsent(anyInt())).thenReturn(pagesOf(List.of(e)));
		when(messagePublisher.kafkaPublish(anyString(), anyString())).thenReturn(true);
		when(repo.markSent(anyString(), any())).thenReturn(true);

		// when
		publisher.flush();

		// then
		verify(messagePublisher).kafkaPublish(eq(e.getPayload()), keyCaptor.capture());
		verify(repo).markSent(idCaptor.capture(), any());

		// key 优先 aggregateId
		org.junit.jupiter.api.Assertions.assertEquals("agg-1", keyCaptor.getValue());
		org.junit.jupiter.api.Assertions.assertEquals("id-1", idCaptor.getValue());

		verify(repo, never()).recordRetry(anyString(), any());
		verify(repo, never()).markDead(anyString(), any());
	}

	@Test
	void flush_failure_recordsRetryWithBackoff() {
		// given
		OutboxEvent e = evt("id-2", "agg-2", "{\"ticketId\":\"t-2\"}", 1, 0, null);
		when(repo.queryUnsent(anyInt())).thenReturn(pagesOf(List.of(e)));
		when(messagePublisher.kafkaPublish(anyString(), anyString())).thenThrow(new RuntimeException("boom"));

		// when
		publisher.flush();

		// then
		verify(repo).recordRetry(eq("id-2"), any());
		verify(repo, never()).markSent(anyString(), any());
		verify(repo, never()).markDead(anyString(), any());
	}

	@Test
	void flush_skipsUntilNextAttemptAtDue() {
		// given: 未到重试时间
		OutboxEvent e = evt("id-3", null, "{\"ticketId\":\"t-3\"}", 2, 0, Instant.now().plusSeconds(60));
		when(repo.queryUnsent(anyInt())).thenReturn(pagesOf(List.of(e)));

		// when
		publisher.flush();

		// then: 不发送、不标记、不重试
		verify(messagePublisher, never()).kafkaPublish(anyString(), anyString());
		verify(repo, never()).markSent(anyString(), any());
		verify(repo, never()).recordRetry(anyString(), any());
		verify(repo, never()).markDead(anyString(), any());
	}

	@Test
	void flush_maxAttempts_marksDead() {
		// given: 达到最大重试
		OutboxEvent e = evt("id-4", "agg-4", "{}", 10, 0, null); // 你的 MAX_ATTEMPTS=10
		when(repo.queryUnsent(anyInt())).thenReturn(pagesOf(List.of(e)));

		// when
		publisher.flush();

		// then
		verify(repo).markDead(eq("id-4"), any());
		verify(messagePublisher, never()).kafkaPublish(anyString(), anyString());
		verify(repo, never()).markSent(anyString(), any());
		verify(repo, never()).recordRetry(anyString(), any());
	}

	@Test
	void flush_extractKey_usesTicketIdWhenNoAggregateId() {
		// given: 没有 aggregateId，用 payload.ticketId
		OutboxEvent e = evt("id-5", null, "{\"ticketId\":\"T-XYZ\"}", 0, 0, null);
		when(repo.queryUnsent(anyInt())).thenReturn(pagesOf(List.of(e)));
		when(messagePublisher.kafkaPublish(anyString(), anyString())).thenReturn(true);
		when(repo.markSent(anyString(), any())).thenReturn(true);

		// when
		publisher.flush();

		// then
		verify(messagePublisher).kafkaPublish(eq(e.getPayload()), keyCaptor.capture());
		org.junit.jupiter.api.Assertions.assertEquals("T-XYZ", keyCaptor.getValue());
	}

	@Test
	void flush_publishReturnsFalse_alsoTriggersRetry() {
		var e = evt("id-x", "agg-x", "{}", 0, 0, null);
		when(repo.queryUnsent(anyInt())).thenReturn(pagesOf(List.of(e)));
		when(messagePublisher.kafkaPublish(anyString(), anyString())).thenReturn(false); // 返回 false 也算失败

		publisher.flush();

		verify(repo).recordRetry(eq("id-x"), any());
		verify(repo, never()).markSent(anyString(), any());
	}

	@Test
	void flush_markSentRace_losesGracefully() {
		var e = evt("id-race", "agg", "{}", 0, 0, null);
		when(repo.queryUnsent(anyInt())).thenReturn(pagesOf(List.of(e)));
		when(messagePublisher.kafkaPublish(anyString(), anyString())).thenReturn(true);
		when(repo.markSent(eq("id-race"), any())).thenReturn(false); // 另一实例已标记成功

		publisher.flush();

		verify(repo).markSent(eq("id-race"), any());
		// 不应 recordRetry/markDead，因为已经被别处标记
		verify(repo, never()).recordRetry(anyString(), any());
		verify(repo, never()).markDead(anyString(), any());
	}
}