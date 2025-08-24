package org.java.ticketingplatform.model;

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
	@JoinColumn(name = "venue_id") // 指定zone表中的外键列
	@ToString.Exclude // 防止在toString()中出现循环引用导致堆栈溢出
	private Venue venue;
}
