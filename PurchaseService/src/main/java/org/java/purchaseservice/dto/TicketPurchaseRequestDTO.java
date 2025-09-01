package org.java.purchaseservice.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketPurchaseRequestDTO {
	private String venueId;
	private String eventId;
	private int zoneId;
	private String row;
	private String column;
}
