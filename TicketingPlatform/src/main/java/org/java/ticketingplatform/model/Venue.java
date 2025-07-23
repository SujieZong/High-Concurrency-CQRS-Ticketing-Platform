package org.java.ticketingplatform.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Venue {
	private String venueId;
	private List<Zone> zones;
}
