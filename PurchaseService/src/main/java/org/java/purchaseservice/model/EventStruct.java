package org.java.purchaseservice.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventStruct {
  private String eventId; // foreign
  private String name;
  private String type;
  private LocalDate date;
  private Venue venueID; // foreign
}
