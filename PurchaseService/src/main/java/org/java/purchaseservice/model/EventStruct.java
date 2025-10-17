package org.java.purchaseservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventStruct {
	private String eventId; //foreign
	private String name;
	private String type;
	private LocalDate date;
	private Venue venueID; //foreign
}
