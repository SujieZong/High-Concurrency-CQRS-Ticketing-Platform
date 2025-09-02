package org.java.rabbitcombinedconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.java.rabbitcombinedconsumer.model.TicketStatus;

import java.time.Instant;

@Data
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
