package org.java.purchaseservice.outbox;

public interface OutboxMessagePublisher {
	boolean kafkaPublish(String payload, String key);
}