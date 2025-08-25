package org.java.ticketingplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

// Following the model TicketCreationDTO
@Data
@AllArgsConstructor
public class TicketRespondDTO {
	private String ticketId;
	private int zoneId;
	private String row;
	private String column;
	private Instant createdOn;
}
