package org.java.purchaseservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
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
