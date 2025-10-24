package org.java.purchaseservice.dto;

import java.time.Instant;
import lombok.*;
import org.java.purchaseservice.model.TicketStatus;

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
	private Instant createdOn;
	private TicketStatus status;       // CREATED / PAID / CANCELLED
}
