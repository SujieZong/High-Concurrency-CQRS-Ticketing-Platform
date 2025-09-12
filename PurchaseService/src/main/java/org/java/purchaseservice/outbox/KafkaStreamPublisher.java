package org.java.purchaseservice.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaStreamPublisher implements OutboxMessagePublisher {

	private final StreamBridge streamBridge;

	@Override
	public boolean kafkaPublish(String payload, String routingKey) {
		var msg = MessageBuilder.withPayload(payload)
				.setHeader("routingKey", routingKey) // yml 配 routing-key-expression 使用此 header
				.build();
		return streamBridge.send("ticket-out-0", msg);
	}
}
