package org.java.purchaseservice.service.dlq;

/** Dead Letter Queue Service Interface. */
public interface DeadLetterQueueService {

  /** Send a failed message payload to the dead letter queue. */
  void sendToDeadLetterQueue(String payload, String partitionKey, String errorReason);
}
