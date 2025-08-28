package org.java.ticketingplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java.ticketingplatform.config.rabbitmq.RabbitFactory;
import org.java.ticketingplatform.domain.OutboxEvent;
import org.java.ticketingplatform.dto.MqDTO;
import org.java.ticketingplatform.outbox.OutboxRelay;
import org.java.ticketingplatform.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

	@Mock
	OutboxEventRepository repo;

	@Mock
	RabbitTemplate rabbit;

	@Mock
	ObjectMapper objectMapper;

	@InjectMocks
	OutboxRelay relay;

	@Test
	void flush_sendsBatchAndMarksSent() throws Exception {
		// 准备两条待发送的 outbox 记录
		OutboxEvent e1 = new OutboxEvent();
		e1.setId(1L);
		e1.setEventType("ticket.created");
		e1.setPayload("{\"p\":1}");
		e1.setSent(false);

		OutboxEvent e2 = new OutboxEvent();
		e2.setId(2L);
		e2.setEventType("ticket.created");
		e2.setPayload("{\"p\":2}");
		e2.setSent(false);

		when(repo.findTop50BySentFalseOrderByIdAsc()).thenReturn(List.of(e1, e2));

		// payload -> MqDTO
		MqDTO dto1 = new MqDTO(
				"t1", "v1", "ev1", 1, "A", "10", "CREATED",
				Instant.parse("2025-08-28T05:45:04.823Z")
		);
		MqDTO dto2 = new MqDTO(
				"t2", "v1", "ev1", 1, "A", "11", "CREATED",
				Instant.parse("2025-08-28T05:45:05Z")
		);
		when(objectMapper.readValue(eq(e1.getPayload()), eq(MqDTO.class))).thenReturn(dto1);
		when(objectMapper.readValue(eq(e2.getPayload()), eq(MqDTO.class))).thenReturn(dto2);

		relay.flush();

		ArgumentCaptor<MqDTO> captor = ArgumentCaptor.forClass(MqDTO.class);
		verify(rabbit, times(2))
				.convertAndSend(eq(RabbitFactory.TICKET_EXCHANGE), eq("ticket.created"), captor.capture());

		List<MqDTO> sentDtos = captor.getAllValues();
		assertThat(sentDtos).hasSize(2);
		assertThat(sentDtos).extracting(MqDTO::getTicketId).containsExactlyInAnyOrder("t1", "t2");

		// 断言：两条记录被标记 sent 并保存
		assertThat(e1.isSent()).isTrue();
		assertThat(e2.isSent()).isTrue();
		verify(repo, times(1)).save(e1);
		verify(repo, times(1)).save(e2);

		verifyNoMoreInteractions(rabbit, repo);
	}

	@Test
	void flush_whenRabbitThrows_doesNotMarkSentOrSave() throws Exception {
		OutboxEvent e = new OutboxEvent();
		e.setId(10L);
		e.setEventType("ticket.created");
		e.setPayload("{\"p\":10}");
		e.setSent(false);

		when(repo.findTop50BySentFalseOrderByIdAsc()).thenReturn(List.of(e));

		MqDTO dto = new MqDTO(
				"t10", "v1", "ev1", 1, "A", "10", "CREATED",
				Instant.parse("2025-08-28T05:45:06Z")
		);
		when(objectMapper.readValue(eq(e.getPayload()), eq(MqDTO.class))).thenReturn(dto);

		// mock MQ error
		doThrow(new RuntimeException("mq down"))
				.when(rabbit)
				.convertAndSend(eq(RabbitFactory.TICKET_EXCHANGE), eq("ticket.created"), any(MqDTO.class));

		relay.flush();

		assertThat(e.isSent()).isFalse();
		verify(repo, never()).save(any());

		verify(rabbit, times(1))
				.convertAndSend(eq(RabbitFactory.TICKET_EXCHANGE), eq("ticket.created"), any(MqDTO.class));

		verifyNoMoreInteractions(rabbit, repo);
	}
}
