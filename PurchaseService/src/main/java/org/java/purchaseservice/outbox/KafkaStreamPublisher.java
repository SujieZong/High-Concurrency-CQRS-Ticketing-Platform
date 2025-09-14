package org.java.purchaseservice.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaStreamPublisher implements OutboxMessagePublisher {

	private final StreamBridge streamBridge;

	@Override
	public boolean kafkaPublish(String payload, String partitionKey) {
		try {
			Message<String> msg = MessageBuilder.withPayload(payload)
					.setHeader("partitionKey", partitionKey)  // 匹配 yml 中的 headers['partitionKey']
					.build();

			boolean sent = streamBridge.send("ticket-out-0", msg);
			log.debug("Message sent to Kafka. PartitionKey: {}, Success: {}", partitionKey, sent);
			return sent;
		} catch (Exception e) {
			log.error("Failed to send message to Kafka", e);
			return false;
		}

//		var msg = MessageBuilder.withPayload(payload)
//				.setHeader("routingKey", routingKey) // yml 配 routing-key-expression 使用此 header
//				.build();
//		return streamBridge.send("ticket-out-0", msg);
	}
}
