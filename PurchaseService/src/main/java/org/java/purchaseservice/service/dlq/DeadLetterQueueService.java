package org.java.purchaseservice.service.dlq;

/**
 * Dead Letter Queue Service Interface
 */
public interface DeadLetterQueueService {
	void sendToDeadLetterQueue(String payload, String partitionKey, String errorReason);

}

