package org.java.purchaseservice.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Venue {
	private String venueId;
	private List<Zone> zones;
}
