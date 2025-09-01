package org.java.purchaseservice.outbox;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.java.purchaseservice.domain.OutboxEvent;
import org.java.purchaseservice.repository.OutboxEventRepository;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {
	private final OutboxEventRepository repo;
	private final StreamBridge streamBridge;

	@Scheduled(fixedDelay = 500) //every 0.5 second scan
	@Transactional
	public void flush() {
		var batch = repo.findTop50BySentFalseOrderByIdAsc();

		for (OutboxEvent e : batch) {
			try {
				var message = org.springframework.messaging.support.MessageBuilder
						.withPayload(e.getPayload()) // String/JSON
						.build();

				boolean ok = streamBridge.send("ticket-out-0", message);
				if (!ok) throw new IllegalStateException("stream send failed");

				e.setSent(true);
				e.setAttempts(e.getAttempts() + 1);
				repo.save(e);
			} catch (Exception ex) {
				e.setAttempts(e.getAttempts() + 1);
				repo.save(e);
			}
		}
	}
}