package org.java.purchaseservice.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Send messages to Kafka through Spring Cloud Stream
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaStreamPublisher implements MessagePublisher {

	private final StreamBridge streamBridge;

	// directly call Binding name from YML instead of hard code.
	@Value("${kafka.binding.ticket-out}")
	private String kafkaBinding;

	@Override
	public boolean kafkaPublish(String payload, String partitionKey) {
		try {
			log.info("【KafkaPublisher】Preparing to send message. binding={}, partitionKey={}, payloadLength={}",
					kafkaBinding, partitionKey, payload.length());
			log.debug("【KafkaPublisher】Message payload: {}", payload);

			Message<String> msg = MessageBuilder.withPayload(payload)
					.setHeader("partitionKey", partitionKey)
					.build();

			boolean sent = streamBridge.send(kafkaBinding, msg);

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
