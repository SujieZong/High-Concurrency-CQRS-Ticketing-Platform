package org.java.purchaseservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "zone")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Zone {
	// Zone 1 - 100, zone 100 $20, prive doubles every 20 zones
	@Id
	@Column(name = "zone_id")
	private int zoneId;

	@Column(name = "ticket_price")
	private BigDecimal ticketPrice;
	private int rowCount;
	private int colCount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "venue_id")
	@ToString.Exclude
	private Venue venue;
}
