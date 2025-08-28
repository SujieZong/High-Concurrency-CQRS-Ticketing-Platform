package org.java.ticketingplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java.ticketingplatform.domain.OutboxEvent;
import org.java.ticketingplatform.outbox.OutboxRelay;
import org.java.ticketingplatform.repository.OutboxEventRepository;
import org.java.ticketingplatform.config.rabbitmq.RabbitFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

	private final ObjectMapper om = new ObjectMapper();

	@Test
	void flush_sendsBatchAndMarksSent() {
		OutboxEventRepository repo = mock(OutboxEventRepository.class);
		RabbitTemplate rabbit = mock(RabbitTemplate.class);


		OutboxRelay relay = new OutboxRelay(repo, rabbit, om);

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

		// Act
		relay.flush();

		// Assert
		verify(rabbit, times(1))
				.convertAndSend(RabbitFactory.TICKET_EXCHANGE, "ticket.created", "{\"p\":1}");
		verify(rabbit, times(1))
				.convertAndSend(RabbitFactory.TICKET_EXCHANGE, "ticket.created", "{\"p\":2}");

		assertThat(e1.isSent()).isTrue();
		assertThat(e2.isSent()).isTrue();
		verify(repo, times(1)).save(e1);
		verify(repo, times(1)).save(e2);
	}

	@Test
	void flush_whenRabbitThrows_doesNotMarkSentOrSave() {
		OutboxEventRepository repo = mock(OutboxEventRepository.class);
		RabbitTemplate rabbit = mock(RabbitTemplate.class);

		OutboxRelay relay = new OutboxRelay(repo, rabbit, om);

		OutboxEvent e = new OutboxEvent();
		e.setId(10L);
		e.setEventType("ticket.created");
		e.setPayload("{\"p\":10}");
		e.setSent(false);

		when(repo.findTop50BySentFalseOrderByIdAsc()).thenReturn(List.of(e));

		// Mock RabbitMQ error
		doThrow(new RuntimeException("mq down"))
				.when(rabbit)
				.convertAndSend(RabbitFactory.TICKET_EXCHANGE, "ticket.created", "{\"p\":10}");

		// Act
		relay.flush();

		assertThat(e.isSent()).isFalse();
		verify(repo, never()).save(e);
	}
}
