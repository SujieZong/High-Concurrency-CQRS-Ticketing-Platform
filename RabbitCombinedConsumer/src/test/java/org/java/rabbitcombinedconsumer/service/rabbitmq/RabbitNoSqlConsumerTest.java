//package org.java.rabbitcombinedconsumer.service.rabbitmq;
//
//import org.java.rabbitcombinedconsumer.model.TicketCreation;
//import org.java.rabbitcombinedconsumer.repository.DynamoTicketDAOInterface;
//import org.java.rabbitcombinedconsumer.repository.dynamo.DynamoTicketDao;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
//import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
//import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
//import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
//import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
//
//import java.time.Instant;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//class RabbitNoSqlConsumerTest {
//
//	@Test
//	void createTicket_success_putsItem_andReturnsId() {
//		// arrange
//		DynamoDbClient ddb = mock(DynamoDbClient.class);
//		when(ddb.putItem(any(PutItemRequest.class)))
//				.thenReturn(PutItemResponse.builder().build());
//
//		DynamoTicketDAOInterface dao = new DynamoTicketDao(ddb);
//
//		TicketCreation t = new TicketCreation();
//		t.setId("d-100");
//		t.setVenueId("v-1");
//		t.setEventId("e-1");
//		t.setZoneId(12);
//		t.setRow("A");
//		t.setColumn("10");
//		t.setStatus("CREATED");
//		Instant created = Instant.parse("2025-08-27T12:34:56Z");
//		t.setCreatedOn(created);
//
//		// act
//		String ret = dao.createTicket(t);
//
//		// assert: return value
//		assertEquals("d-100", ret);
//
//		// capture the actual PutItemRequest sent to DynamoDbClient
//		ArgumentCaptor<PutItemRequest> cap = ArgumentCaptor.forClass(PutItemRequest.class);
//		verify(ddb, times(1)).putItem(cap.capture());
//
//		PutItemRequest req = cap.getValue();
//		assertEquals("Tickets", req.tableName());
//		assertEquals("attribute_not_exists(ticketId)", req.conditionExpression());
//
//		Map<String, AttributeValue> item = req.item();
//		assertNotNull(item);
//
//		// field-by-field assertions (types matter)
//		assertTrue(item.containsKey("ticketId"));
//		assertEquals("d-100", item.get("ticketId").s());
//
//		assertTrue(item.containsKey("venueId"));
//		assertEquals("v-1", item.get("venueId").s());
//
//		assertTrue(item.containsKey("eventId"));
//		assertEquals("e-1", item.get("eventId").s());
//
//		assertTrue(item.containsKey("zoneId"));
//		assertEquals("12", item.get("zoneId").n()); // numeric
//
//		assertTrue(item.containsKey("row"));
//		assertEquals("A", item.get("row").s());
//
//		assertTrue(item.containsKey("column"));
//		assertEquals("10", item.get("column").s());
//
//		if (item.containsKey("status")) {
//			assertEquals("CREATED", item.get("status").s());
//		}
//
//		assertTrue(item.containsKey("createdOn"));
//		assertEquals(created.toString(), item.get("createdOn").s());
//	}
//
//	@Test
//	void createTicket_idempotent_duplicate_doesNotThrow_andReturnsId() {
//		// arrange
//		DynamoDbClient ddb = mock(DynamoDbClient.class);
//		when(ddb.putItem(any(PutItemRequest.class)))
//				.thenThrow(ConditionalCheckFailedException.builder().message("exists").build());
//
//		DynamoTicketDAOInterface dao = new DynamoTicketDao(ddb);
//
//		TicketCreation t = new TicketCreation();
//		t.setId("d-dup");
//		t.setVenueId("v-1");
//		t.setEventId("e-1");
//		t.setZoneId(1);
//		t.setRow("B");
//		t.setColumn("2");
//		t.setStatus("CREATED");
//		t.setCreatedOn(Instant.parse("2025-08-27T00:00:00Z"));
//
//		// act
//		String ret = dao.createTicket(t);
//
//		assertEquals("d-dup", ret);
//		verify(ddb, times(1)).putItem(any(PutItemRequest.class));
//	}
//}
