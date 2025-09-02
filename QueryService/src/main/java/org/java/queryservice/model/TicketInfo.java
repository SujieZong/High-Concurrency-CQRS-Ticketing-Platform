package org.java.queryservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "ticket")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketInfo {
	@Id
	@Column(name = "ticket_id")
	private String ticketId;

	@Column(name = "venue_id")
	private String venueId;

	@Column(name = "event_id")
	private String eventId;

	@Column(name = "zone_id")
	private int zoneId;

	@Column(name = "row_label")
	private String row;

	@Column(name = "col_label")
	private String column;

	@Column(name = "status", nullable = false, length = 16)
	@Enumerated(EnumType.STRING)
	private TicketStatus status;

	// have restrictions on changing this date
	@Column(name = "created_on", nullable = false, updatable = false)
	private Instant createdOn;
}