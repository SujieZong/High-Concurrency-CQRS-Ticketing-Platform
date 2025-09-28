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
			log.info("【KafkaPublisher】Preparing to send message. partitionKey={}, payloadLength={}",
					partitionKey, payload.length());
			log.debug("【KafkaPublisher】Message payload: {}", payload);

			Message<String> msg = MessageBuilder.withPayload(payload)
					.setHeader("partitionKey", partitionKey) // 匹配 yml 中的 headers['partitionKey']
					.build();

			boolean sent = streamBridge.send("ticket-out-0", msg);

			if (sent) {
				log.info("【KafkaPublisher】Message sent to Kafka successfully. partitionKey={}", partitionKey);
			} else {
				log.error("【KafkaPublisher】Failed to send message to Kafka. partitionKey={}", partitionKey);
			}

			return sent;
		} catch (Exception e) {
			log.error("【KafkaPublisher】Exception occurred while sending message to Kafka. partitionKey={}",
					partitionKey, e);
			return false;
		}
	}
}
