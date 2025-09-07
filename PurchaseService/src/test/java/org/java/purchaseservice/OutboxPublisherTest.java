//package org.java.purchaseservice;
//
//import org.java.purchaseservice.domain.OutboxEvent;
//import org.java.purchaseservice.outbox.OutboxPublisher;
//import org.java.purchaseservice.repository.OutboxEventRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.cloud.stream.function.StreamBridge;
//import org.springframework.messaging.Message;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class OutboxPublisherTest {
//
//	@Mock
//	StreamBridge streamBridge;
//	@Mock
//	OutboxEventRepository repo;
//
//	@InjectMocks
//	OutboxPublisher publisher;
//
//	@Test
//	void flush_sendsBatchAndMarksSent() {
//		OutboxEvent e1 = new OutboxEvent();
//		e1.setPayload("{\"p\":1}");
//		OutboxEvent e2 = new OutboxEvent();
//		e2.setPayload("{\"p\":2}");
//		when(repo.findTop50BySentFalseOrderByIdAsc()).thenReturn(List.of(e1, e2));
//		when(streamBridge.send(eq("ticket-out-0"), any(Message.class))).thenReturn(true);
//
//		publisher.flush();
//
//		verify(streamBridge, times(2)).send(eq("ticket-out-0"), any(Message.class));
//		assertThat(e1.isSent()).isTrue();
//		assertThat(e2.isSent()).isTrue();
//		assertThat(e1.getAttempts()).isEqualTo(1);
//		assertThat(e2.getAttempts()).isEqualTo(1);
//		verify(repo).save(e1);
//		verify(repo).save(e2);
//	}
//
//	@Test
//	void flush_whenSendFails_attemptsInc_butNotSent() {
//		OutboxEvent e = new OutboxEvent();
//		e.setPayload("{\"p\":10}");
//		when(repo.findTop50BySentFalseOrderByIdAsc()).thenReturn(List.of(e));
//		when(streamBridge.send(eq("ticket-out-0"), any(Message.class))).thenReturn(false);
//
//		publisher.flush();
//
//		assertThat(e.isSent()).isFalse();
//		assertThat(e.getAttempts()).isEqualTo(1);
//		verify(repo).save(e);
//	}
//}