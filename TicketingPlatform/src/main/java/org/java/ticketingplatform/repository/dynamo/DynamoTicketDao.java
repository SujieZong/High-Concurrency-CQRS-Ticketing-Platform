package org.java.ticketingplatform.repository.dynamo;

import lombok.extern.slf4j.Slf4j;
import org.java.ticketingplatform.model.TicketInfo;
import org.java.ticketingplatform.model.TicketStatus;
import org.java.ticketingplatform.repository.DynamoTicketDAOInterface;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Slf4j
@Repository
public class DynamoTicketDao implements DynamoTicketDAOInterface {

	private static final String dynamoTABLE_NAME = "Tickets";
	private final DynamoDbClient dynamoDbClient;

	public DynamoTicketDao(DynamoDbClient dynamoDbClient) {
		this.dynamoDbClient = dynamoDbClient;
	}

	@Override
	public TicketInfo getTicketInfoById(String id) {
		log.debug("[DynamoTicketDao] getTicketInfoById called for id={}", id);

		GetItemRequest getReq = GetItemRequest.builder()
				.tableName(dynamoTABLE_NAME)
				.key(Map.of("ticketId", AttributeValue.fromS(id)))
				.build();

		log.debug("[DynamoTicketDao] Executing getItem request: {}", getReq);
		Map<String, AttributeValue> item = dynamoDbClient.getItem(getReq).item();

		if (item == null || item.isEmpty()) {
			return null;
		}

		log.debug("[DynamoTicketDao] Retrieved item: {}", item);

		String ticketId = item.get("ticketId").s();
		String venueId = item.get("venueId").s();
		String eventId = item.get("eventId").s();
		int zoneId = Integer.parseInt(item.get("zoneId").n());
		String row = item.get("row").s();
		String column = item.get("column").s();
		String createdOnString = item.get("createdOn").s();
		String statusString = item.get("status").s();

		TicketStatus status;
		try {
			status = TicketStatus.valueOf(statusString);
		} catch (IllegalArgumentException ex) {
			log.warn("[DynamoTicketDao] Unknown status='{}' ticketId={}. Fallback=CREATED", statusString, ticketId);
			status = TicketStatus.PAID;
		}

		Instant createdOn;
		try {
			createdOn = Instant.parse(createdOnString);
		} catch (DateTimeParseException e) {
			log.error("[DynamoTicketDao] Bad createdOn='{}' ticketId={}. Using now()", createdOnString, ticketId, e);
			createdOn = Instant.now();
		}

		TicketInfo info = new TicketInfo(ticketId, venueId, eventId, zoneId, row, column, createdOn, status);
		log.debug("[DynamoTicketDao] Mapped TicketInfo: {}", info);
		return info;
	}
}
