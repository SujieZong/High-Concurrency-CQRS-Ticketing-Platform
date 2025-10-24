package org.java.purchaseservice.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketInfo {
	private String ticketId;
	private String venueId;
	private String eventId;
	private int zoneId;
	private String row;
	private String column;
	private TicketStatus status;
	private Instant createdOn;
}