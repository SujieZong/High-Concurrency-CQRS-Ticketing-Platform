package org.java.purchaseservice.outbox;

public interface OutboxMessagePublisher {
	boolean rabbitPublish(String payload, String key);
}