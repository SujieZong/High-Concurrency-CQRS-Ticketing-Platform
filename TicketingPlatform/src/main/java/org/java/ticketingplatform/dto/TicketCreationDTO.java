package org.java.ticketingplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class TicketCreationDTO {
	private String id;
	private String venueId;
	private String eventId;
	private int zoneId;
	private String row;
	private String column;
	private Instant createdOn;
}
