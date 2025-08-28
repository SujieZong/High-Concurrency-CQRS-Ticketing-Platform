package org.java.rabbitcombinedconsumer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class TicketCreation {
	private String id;
	private String venueId;
	private String eventId;
	private int zoneId;
	private String row;
	private String column;
	private String status; //"CREATED", "PAID", "CANCELLED"
	private Instant createdOn;
}
