package org.java.ticketingplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TicketPurchaseRequestDTO {
	private String venueId;
	private String eventId;
	private int zoneId;
	private String row;
	private String column;
}
