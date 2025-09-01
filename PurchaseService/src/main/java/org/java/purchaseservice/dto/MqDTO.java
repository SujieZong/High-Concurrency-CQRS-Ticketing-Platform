package org.java.purchaseservice.dto;

import lombok.*;
import org.java.purchaseservice.model.TicketStatus;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MqDTO {
	private String ticketId;
	private String venueId;
	private String eventId;
	private int zoneId;
	private String row;
	private String column;
	private TicketStatus status;       // CREATED / PAID / CANCELLED
	private Instant createdOn;
}
