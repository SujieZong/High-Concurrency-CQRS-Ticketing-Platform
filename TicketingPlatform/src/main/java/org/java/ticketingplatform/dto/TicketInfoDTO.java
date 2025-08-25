package org.java.ticketingplatform.dto;

import lombok.*;
import org.java.ticketingplatform.model.TicketStatus;

import java.time.Instant;

// Following the model TicketCreationDTO
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TicketInfoDTO {
	private String ticketId;
	private String venueId;
	private String eventId;
	private int zoneId;
	private String row;
	private String column;
	private TicketStatus status; //"CREATED", "PAID", "CANCELLED"
	private Instant createdOn;
}
