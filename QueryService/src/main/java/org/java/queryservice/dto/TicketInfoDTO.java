package org.java.queryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.java.queryservice.model.TicketStatus;

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
