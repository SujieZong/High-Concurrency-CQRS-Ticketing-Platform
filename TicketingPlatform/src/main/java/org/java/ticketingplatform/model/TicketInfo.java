package org.java.ticketingplatform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

	@Column(name = "col_label")
	private String column;

	@Column(name = "row_label")
	private String row;
	// have restrictions on changing this date
	@Column(name = "created_on", nullable = false, updatable = false)
	private Instant createdOn;
}