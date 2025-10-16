package org.java.purchaseservice.event;

import lombok.Builder;
import lombok.Data;
import org.java.purchaseservice.model.TicketStatus;

import java.time.Instant;

@Data
@Builder
public class TicketCreatedEvent {
	private String ticketId;
	private String venueId;
	private String eventId;
	private Integer zoneId;
	private String row;
	private String column;
	private TicketStatus status;
	private Instant createdOn;

	public String getPartitionKey() {
		return venueId;
	}

}
