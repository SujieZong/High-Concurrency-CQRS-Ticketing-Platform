package org.java.ticketingplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class MqDTO {
	private String ticketId;
	private String venueId;
	private String eventId;
	private int zoneId;
	private String row;
	private String column;
	private String status;       // CREATED / PAID / CANCELLED
	private Instant createdOn;
}
