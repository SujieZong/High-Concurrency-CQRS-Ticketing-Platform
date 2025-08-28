package org.java.ticketingplatform.outbox;

import lombok.RequiredArgsConstructor;
import org.java.ticketingplatform.domain.OutboxEvent;
import org.java.ticketingplatform.repository.OutboxEventRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxRelay {
	private final OutboxEventRepository repo;
	private final RabbitTemplate rabbit;

	@Scheduled(fixedDelay = 500) // 每0.5秒扫一次
	public void flush() {
		List<OutboxEvent> batch = repo.findTop50BySentFalseOrderByIdAsc();
		for (OutboxEvent e : batch) {
			try {
				rabbit.convertAndSend("ticket.events", "ticket.created", e.getPayload());
				e.setSent(true);
				repo.save(e);
			} catch (Exception ex) {
				// 留给下一轮重试
			}
		}
	}
}
