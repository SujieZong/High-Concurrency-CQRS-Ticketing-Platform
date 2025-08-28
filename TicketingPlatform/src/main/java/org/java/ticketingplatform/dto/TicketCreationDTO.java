package org.java.ticketingplatform.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TicketCreationDTO {
	private String id;
	private String venueId;
	private String eventId;
	private int zoneId;
	private String row;
	private String column;
	private Instant createdOn;
}
