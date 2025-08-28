package org.java.rabbitcombinedconsumer.service.rabbitmq;

import org.java.rabbitcombinedconsumer.model.TicketCreation;
import org.java.rabbitcombinedconsumer.repository.DynamoTicketDAOInterface;
import org.java.rabbitcombinedconsumer.repository.dynamo.DynamoTicketDao;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class RabbitNoSqlConsumerTest {

	@Test
	void createTicket_success_putsItem_andReturnsId() {
		// arrange
		DynamoDbClient ddb = mock(DynamoDbClient.class);
		when(ddb.putItem(any(PutItemRequest.class)))
				.thenReturn(PutItemResponse.builder().build());

		DynamoTicketDAOInterface dao = new DynamoTicketDao(ddb);


		TicketCreation t = new TicketCreation();
		t.setId("d-100");
		t.setVenueId("v-1");
		t.setEventId("e-1");
		t.setZoneId(12);
		t.setRow("A");
		t.setColumn("10");
		t.setStatus("CREATED");
		Instant created = Instant.parse("2025-08-27T12:34:56Z");
		t.setCreatedOn(created);

		// act
		String ret = dao.createTicket(t);

		// assert
		assertEquals("d-100", ret);

		ArgumentCaptor<PutItemRequest> cap = ArgumentCaptor.forClass(PutItemRequest.class);
		verify(ddb, times(1)).putItem(cap.capture());

		PutItemRequest req = cap.getValue();
		assertEquals("Tickets", req.tableName());
		assertEquals("attribute_not_exists(ticketId)", req.conditionExpression());

		Map<String, AttributeValue> item = req.item();
		assertNotNull(item);
		// 字段映射校验
		assertEquals("d-100", item.get("ticketId").s());
		assertEquals("v-1", item.get("venueId").s());
		assertEquals("e-1", item.get("eventId").s());
		assertEquals("12", item.get("zone").n());
		assertEquals("A", item.get("row").s());
		assertEquals("10", item.get("column").s());
		assertEquals("CREATED", item.get("status").s());
		// createdTime 使用 toString() 的 ISO-8601
		assertEquals(created.toString(), item.get("createdTime").s());
	}

	@Test
	void createTicket_idempotent_duplicate_doesNotThrow_andReturnsId() {
		// arrange
		DynamoDbClient ddb = mock(DynamoDbClient.class);
		when(ddb.putItem(any(PutItemRequest.class)))
				.thenThrow(ConditionalCheckFailedException.builder().message("exists").build());

		DynamoTicketDAOInterface dao = new DynamoTicketDao(ddb);

		TicketCreation t = new TicketCreation();
		t.setId("d-dup");
		t.setVenueId("v-1");
		t.setEventId("e-1");
		t.setZoneId(1);
		t.setRow("B");
		t.setColumn("2");
		t.setStatus("CREATED");
		t.setCreatedOn(Instant.parse("2025-08-27T00:00:00Z"));

		// act
		String ret = dao.createTicket(t);

		// assert：不抛异常，并返回相同 id
		assertEquals("d-dup", ret);
		verify(ddb, times(1)).putItem(any(PutItemRequest.class));
	}
}
