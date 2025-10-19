package org.java.mqprojectionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.java.mqprojectionservice.model.TicketStatus;

import java.time.Instant;

// Mirrors the Kafka payload produced from PurchaseService TicketCreatedEvent
@Data
@AllArgsConstructor
public class TicketInfoDTO {
	private String venueId;
	private String eventId;
	private int zoneId;
	private String row;
	private String column;
	private TicketStatus status;
	private Instant createdOn;
}
