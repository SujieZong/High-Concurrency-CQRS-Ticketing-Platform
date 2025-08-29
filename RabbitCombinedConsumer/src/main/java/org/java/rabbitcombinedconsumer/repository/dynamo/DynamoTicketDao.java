//package org.java.rabbitcombinedconsumer.repository.dynamo;
//
//import lombok.extern.slf4j.Slf4j;
//import org.java.rabbitcombinedconsumer.model.TicketCreation;
//import org.java.rabbitcombinedconsumer.repository.DynamoTicketDAOInterface;
//import org.springframework.stereotype.Repository;
//import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
//import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
//import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
//import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Slf4j
//@Repository
//public class DynamoTicketDao implements DynamoTicketDAOInterface {
//
//	private static final String dynamoTABLE_NAME = "Tickets";
//	private final DynamoDbClient dynamoDbClient;
//
//	public DynamoTicketDao(DynamoDbClient dynamoDbClient) {
//		this.dynamoDbClient = dynamoDbClient;
//	}
//
//	public String createTicket(TicketCreation ticket) {
//		log.debug("[DynamoTicketDao] createTicket called with: {}", ticket);
//
//		Map<String, AttributeValue> item = new HashMap<>();
//		item.put("ticketId", AttributeValue.fromS(ticket.getId()));
//		item.put("venueId", AttributeValue.fromS(ticket.getVenueId()));
//		item.put("eventId", AttributeValue.fromS(ticket.getEventId()));
//		item.put("zoneId", AttributeValue.fromN(String.valueOf(ticket.getZoneId())));
//		item.put("row", AttributeValue.fromS(ticket.getRow()));
//		item.put("column", AttributeValue.fromS(ticket.getColumn()));
//		item.put("status", AttributeValue.fromS(ticket.getStatus()));
//
//
//		String timeIso = ticket.getCreatedOn().toString(); // process the time to iso time type
//		item.put("createdOn", AttributeValue.fromS(timeIso));
//
//		log.debug("[DynamoTicketDao] Prepared item map for putItem: {}", item);
//
//
//		PutItemRequest request = PutItemRequest.builder()
//				.tableName(dynamoTABLE_NAME)
//				.item(item)
//				.conditionExpression("attribute_not_exists(ticketId)")
//				.build();
//
//		log.debug("[DynamoTicketDao] Executing putItem request: {}", request);
//		try {
//			dynamoDbClient.putItem(request);
//			log.debug("[DynamoTicketDao] Successfully persisted ticket with id={}", ticket.getId());
//		} catch (ConditionalCheckFailedException e) {
//			log.warn("[DynamoTicketDao] ticketId={} Idempotent Ignore", ticket.getId());
//		}
//		return ticket.getId();
//	}
//}
//
