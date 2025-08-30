package org.java.purchaseservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "event")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {
	@Id
	@Column(name = "event_Id")
	private String eventId; //foreign
	@Column(name = "name")
	private String name;
	@Column(name = "type")
	private String type;
	@Column(name = "event_date")
	private LocalDate date;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "venue_id", nullable = false)
	@ToString.Exclude
	private Venue venueID; //foreign
}
