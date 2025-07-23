package org.java.rabbitcombinedconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

// Following the model TicketCreation
@Data
@AllArgsConstructor
public class TicketInfoDTO {
	private String venueId;
	private String eventId;
	private int zoneId;
	private String row;
	private String column;
	private Instant createdOn;
}
