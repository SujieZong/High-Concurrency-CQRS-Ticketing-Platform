package org.java.ticketingplatform.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class Event {
	private String eventId; //foreign
	private String venueID; //foreign
	private String name;
	private String type;
	private LocalDate date;
}
