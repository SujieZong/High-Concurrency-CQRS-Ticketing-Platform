package org.java.purchaseservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.java.purchaseservice.config.rabbitmq.RabbitFactory;
import org.java.purchaseservice.domain.OutboxEvent;
import org.java.purchaseservice.dto.MqDTO;
import org.java.purchaseservice.repository.OutboxEventRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxRelay {
	private final OutboxEventRepository repo;
	private final RabbitTemplate rabbit;
	private final ObjectMapper objectMapper;


	@Scheduled(fixedDelay = 500) //every 0.5 second scan
	public void flush() {
		List<OutboxEvent> batch = repo.findTop50BySentFalseOrderByIdAsc();
		for (OutboxEvent e : batch) {
			try {
				MqDTO dto = objectMapper.readValue(e.getPayload(), MqDTO.class);
				rabbit.convertAndSend(RabbitFactory.TICKET_EXCHANGE, "ticket.created", dto);
				e.setSent(true);
				repo.save(e);
			} catch (Exception ex) {
				// 留给下一轮重试
			}
		}
	}
}
