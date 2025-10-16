// package org.java.purchaseservice.repository.dynamo;

// import lombok.extern.slf4j.Slf4j;
// import org.java.purchaseservice.model.TicketInfo;
// import org.java.purchaseservice.repository.DynamoTicketDaoInterface;
// import org.springframework.stereotype.Repository;
// import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
// import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
// import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
// import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

// import java.util.HashMap;
// import java.util.Map;

// @Slf4j
// @Repository
// public class DynamoTicketDao implements DynamoTicketDaoInterface {

// 	private static final String dynamoTABLE_NAME = "Tickets";
// 	private final DynamoDbClient dynamoDbClient;

// 	public DynamoTicketDao(DynamoDbClient dynamoDbClient) {
// 		this.dynamoDbClient = dynamoDbClient;
// 	}

// 	@Override
// 	public void createTicket(TicketInfo ticket) {
// 		log.debug("[DynamoTicketDao] createTicket called with: {}", ticket);

// 		Map<String, AttributeValue> item = new HashMap<>();
// 		item.put("ticketId", AttributeValue.fromS(ticket.getTicketId()));
// 		item.put("venueId", AttributeValue.fromS(ticket.getVenueId()));
// 		item.put("eventId", AttributeValue.fromS(ticket.getEventId()));
// 		item.put("zoneId", AttributeValue.fromN(String.valueOf(ticket.getZoneId())));
// 		item.put("row", AttributeValue.fromS(ticket.getRow()));
// 		item.put("column", AttributeValue.fromS(ticket.getColumn()));
// 		item.put("status", AttributeValue.fromS(ticket.getStatus().name()));
// 		item.put("createdOn", AttributeValue.fromS(ticket.getCreatedOn().toString()));

// 		log.debug("[DynamoTicketDao] Prepared item map for putItem: {}", item);

// 		PutItemRequest request = PutItemRequest.builder()
// 				.tableName(dynamoTABLE_NAME)
// 				.item(item)
// 				.conditionExpression("attribute_not_exists(ticketId)")
// 				.build();

// 		log.debug("[DynamoTicketDao] Executing putItem request: {}", request);

// 		try {
// 			dynamoDbClient.putItem(request);
// 			log.debug("[DynamoTicketDao] Successfully persisted ticket with id={}", ticket.getTicketId());
// 		} catch (ConditionalCheckFailedException e) {
// 			log.warn("[DynamoTicketDao] ticketId={} Idempotent Ignore", ticket.getTicketId());
// 		}
// 	}
// }
