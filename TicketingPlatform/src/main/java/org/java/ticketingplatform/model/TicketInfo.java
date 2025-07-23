package org.java.ticketingplatform.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class TicketInfo {
	private String ticketId;
	private String venueId;
	private String eventId;
	private int zoneId;
	private String column;
	private String row;
	private Instant createdOn;
}