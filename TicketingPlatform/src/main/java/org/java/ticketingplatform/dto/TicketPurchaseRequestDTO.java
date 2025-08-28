package org.java.ticketingplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
public class TicketPurchaseRequestDTO {
	private String venueId;
	private String eventId;
	private int zoneId;
	private String row;
	private String column;
}
