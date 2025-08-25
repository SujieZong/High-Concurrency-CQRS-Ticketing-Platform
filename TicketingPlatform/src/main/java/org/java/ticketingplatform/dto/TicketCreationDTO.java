package org.java.ticketingplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class TicketCreationDTO {
	private String id;
	private String venueId;
	private String eventId;
	private int zoneId;
	private String column;
	private String row;
	private Instant createdOn;
}
