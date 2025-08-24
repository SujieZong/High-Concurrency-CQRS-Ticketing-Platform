package org.java.ticketingplatform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "venue")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Venue {
	@Id
	@Column(name = "venue_id")
	private String venueId;

	@OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, fetch =  FetchType.LAZY)
	private List<Zone> zones;
}
