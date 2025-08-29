package org.java.ticketingplatform.dto;

import lombok.*;

@Data
@AllArgsConstructor
public class TicketPurchaseRequestDTO {
	private String venueId;
	private String eventId;
	private int zoneId;
	private String row;
	private String column;
}
