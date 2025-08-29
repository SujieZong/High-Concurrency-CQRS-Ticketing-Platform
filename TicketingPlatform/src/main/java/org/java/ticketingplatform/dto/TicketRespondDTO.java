package org.java.ticketingplatform.dto;

import lombok.*;

import java.time.Instant;

// Following the model TicketCreationDTO
@Getter
@Setter
@AllArgsConstructor
public class TicketRespondDTO {
	private String ticketId;
	private int zoneId;
	private String row;
	private String column;
	private Instant createdOn;
}
