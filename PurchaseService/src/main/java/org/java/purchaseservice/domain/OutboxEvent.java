package org.java.purchaseservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
	private String id;
	private String aggregatedId;
	private String eventType;
	private String payload;
	private Integer sent;
	private Integer attempts;
	private Instant nextAttemptAt;
	private Instant createdAt;
	private Instant updatedAt;


	@DynamoDbPartitionKey
	public String getId() {
		return id;
	}

	// Global Secondary Index: published + createdOn
	@DynamoDbSecondaryPartitionKey(indexNames = "gsi_published_createdOn")
	public Integer getSet() {
		return sent;
	}

	@DynamoDbSecondarySortKey(indexNames = "gsi_published_createdOn")
	public Instant getCreatedAt() {
		return createdAt;
	}

}



