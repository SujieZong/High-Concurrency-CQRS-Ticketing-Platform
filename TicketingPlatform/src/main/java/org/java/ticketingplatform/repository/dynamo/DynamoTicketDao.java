package org.java.ticketingplatform.repository.dynamo;

import lombok.extern.slf4j.Slf4j;
import org.java.ticketingplatform.dto.TicketCreationDTO;
import org.java.ticketingplatform.repository.DynamoTicketDAOInterface;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Repository
public class DynamoTicketDao implements DynamoTicketDAOInterface {

	private static final String dynamoTABLE_NAME = "Tickets";
	private final DynamoDbClient dynamoDbClient;

	public DynamoTicketDao(DynamoDbClient dynamoDbClient) {
		this.dynamoDbClient = dynamoDbClient;
	}

	public String createTicket(TicketCreationDTO ticket) {
		log.debug("[DynamoTicketDao] createTicket called with: {}", ticket);

		Map<String, AttributeValue> item = new HashMap<>();
		item.put("ticketId", AttributeValue.fromS(ticket.getId()));
		item.put("venueId", AttributeValue.fromS(ticket.getVenueId()));
		item.put("eventId", AttributeValue.fromS(ticket.getEventId()));
		item.put("zoneId", AttributeValue.fromN(String.valueOf(ticket.getZoneId())));
		item.put("row", AttributeValue.fromS(ticket.getRow()));
		item.put("column", AttributeValue.fromS(ticket.getColumn()));
		item.put("status", AttributeValue.fromS(ticket.getStatus()));

		String timeIso = ticket.getCreatedOn().toString(); // process the time to iso time type
		item.put("createdOn", AttributeValue.fromS(timeIso));

		log.debug("[DynamoTicketDao] Prepared item map for putItem: {}", item);


		PutItemRequest request = PutItemRequest.builder()
				.tableName(dynamoTABLE_NAME)
				.item(item)
				.conditionExpression("attribute_not_exists(ticketId)")
				.build();

		log.debug("[DynamoTicketDao] Executing putItem request: {}", request);
		try {
			dynamoDbClient.putItem(request);
			log.debug("[DynamoTicketDao] Successfully persisted ticket with id={}", ticket.getId());
		} catch (ConditionalCheckFailedException e) {
			log.warn("[DynamoTicketDao] ticketId={} Idempotent Ignore", ticket.getId());
		}
		return ticket.getId();
	}
}

//
//@Override
//	public TicketInfo getTicketInfoById(String id) {
//		log.debug("[DynamoTicketDao] getTicketInfoById called for id={}", id);
//
//		GetItemRequest getReq = GetItemRequest.builder()
//				.tableName(dynamoTABLE_NAME)
//				.key(Map.of("ticketId", AttributeValue.fromS(id)))
//				.build();
//
//		log.debug("[DynamoTicketDao] Executing getItem request: {}", getReq);
//		Map<String, AttributeValue> item = dynamoDbClient.getItem(getReq).item();
//
//		if (item == null || item.isEmpty()) {
//			return null;
//		}
//
//		log.debug("[DynamoTicketDao] Retrieved item: {}", item);
//
//		String ticketId = item.get("ticketId").s();
//		String venueId = item.get("venueId").s();
//		String eventId = item.get("eventId").s();
//		int zoneId = Integer.parseInt(item.get("zoneId").n());
//		String row = item.get("row").s();
//		String column = item.get("column").s();
//		String createdOnString = item.get("createdOn").s();
//		String statusString = item.get("status").s();
//
//		TicketStatus status;
//		try {
//			status = TicketStatus.valueOf(statusString);
//		} catch (IllegalArgumentException ex) {
//			log.warn("[DynamoTicketDao] Unknown status='{}' ticketId={}. Fallback=CREATED", statusString, ticketId);
//			status = TicketStatus.PAID;
//		}
//
//		Instant createdOn;
//		try {
//			createdOn = Instant.parse(createdOnString);
//		} catch (DateTimeParseException e) {
//			log.error("[DynamoTicketDao] Bad createdOn='{}' ticketId={}. Using now()", createdOnString, ticketId, e);
//			createdOn = Instant.now();
//		}
//
//		TicketInfo info = new TicketInfo(ticketId, venueId, eventId, zoneId, row, column, createdOn, status);
//		log.debug("[DynamoTicketDao] Mapped TicketInfo: {}", info);
//		return info;
//	}

