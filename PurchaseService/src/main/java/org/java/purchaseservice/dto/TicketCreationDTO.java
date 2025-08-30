package org.java.purchaseservice.dto;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketCreationDTO {
	private String id;
	private String venueId;
	private String eventId;
	private int zoneId;
	private String row;
	private String column;
	private String status; //"CREATED", "PAID", "CANCELLED"
	private Instant createdOn;
}
