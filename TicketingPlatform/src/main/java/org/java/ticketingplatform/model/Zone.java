package org.java.ticketingplatform.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Zone {
	// Zone 1 - 100, zone 100 $20, prive doubles every 20 zones
	private int zoneId;
	private BigDecimal ticketPrice;
	private int rowCount;
	private int colCount;
}
