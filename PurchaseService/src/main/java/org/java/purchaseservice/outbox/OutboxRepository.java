package org.java.purchaseservice.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Repository
@RequiredArgsConstructor
public class OutboxRepository {
	private final DynamoDbEnhancedClient enhanced;

	@Value("${app.dynamo.table.outbox:OutboxEvent}")
	private String tableName;

	// bind table name to a variable, decouple
	private static final String GSI_SENT_CREATED_AT = "gsi_sent_createdAt";

	private DynamoDbTable<OutboxEvent> table() {
		return enhanced.table(tableName, TableSchema.fromBean(OutboxEvent.class));
	}

	// write an outbox entry, sent = 0 attempts = 0
	public String save(String eventType, String payload, String aggregateId) {
		var now = Instant.now();
		var e = OutboxEvent.builder()
				.id(UUID.randomUUID().toString())
				.aggregateId(aggregateId)
				.eventType(eventType)
				.payload(payload)
				.sent(0)   // 0 for not published
				.attempts(0)
				.createdAt(now)
				.updatedAt(now)
				.build();
		table().putItem(e);
		return e.getId();
	}

	/**
	 * check unsent entry, scan page by time
	 */
	public PageIterable<OutboxEvent> queryUnsent(int pageSize) {
		var idx = table().index(GSI_SENT_CREATED_AT);
		var builder = QueryEnhancedRequest.builder()
				.queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(0).build()))
				.limit(pageSize)
				.scanIndexForward(true);

		return PageIterable.create(idx.query(builder.build()));
	}

	/**
	 * After entry is sent, update 0 to 1, return success or failed
	 */
	public boolean markSent(String id, Instant sentTime) {
		var key = Key.builder().partitionValue(id).build();
		var current = table().getItem(r -> r.key(key));
		if (current == null || current.getSent() != 0) return false;

		current.setSent(1);
		current.setUpdatedAt(sentTime);

		try {
			table().updateItem(UpdateItemEnhancedRequest.builder(OutboxEvent.class)
					.item(current)
					.conditionExpression(Expression.builder()
							.expression("sent = :s0")
							.expressionValues(Map.of(":s0", AttributeValue.fromN("0")))
							.build())
					.build());
			return true;
		} catch (ConditionalCheckFailedException e) {
			return false;
		}
	}

	/**
	 * If failed, attempt ++
	 */
	public void recordRetry(String id, Function<Integer, Instant> nextAttemptPolicy) {
		var key = Key.builder().partitionValue(id).build();  // equles SQL: select * from outboxevent where id = :id
		var item = table().getItem(r -> r.key(key)); // try to find item by key
		if (item == null) return; //if not found

		int attempts = item.getAttempts() == null ? 0 : item.getAttempts(); //get item retry times default 0
		item.setAttempts(attempts + 1);
		item.setNextAttemptAt(nextAttemptPolicy.apply(attempts + 1));
		item.setUpdatedAt(Instant.now());

		table().updateItem(item);
	}

	public boolean markDead(String id, Instant time) {
		var key = Key.builder().partitionValue(id).build();
		var current = table().getItem(r -> r.key(key));
		if (current == null || current.getSent() == null || current.getSent() != 0) return false;

		current.setSent(2); // 2 means failed, stop trying
		current.setUpdatedAt(time);
		table().updateItem(current);
		return true;
	}
}