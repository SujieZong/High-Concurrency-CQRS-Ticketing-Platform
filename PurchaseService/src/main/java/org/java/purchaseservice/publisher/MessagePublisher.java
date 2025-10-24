package org.java.purchaseservice.publisher;

/** Message Publish Interface */
public interface MessagePublisher {
  boolean kafkaPublish(String payload, String partitionKey);
}
