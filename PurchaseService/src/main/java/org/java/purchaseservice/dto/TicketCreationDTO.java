package org.java.purchaseservice.dto;

import java.time.Instant;
import lombok.*;
import org.java.purchaseservice.model.TicketStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketCreationDTO {
  private String ticketId;
  private String venueId;
  private String eventId;
  private int zoneId;
  private String row;
  private String column;
  private TicketStatus status; // "CREATED", "PAID", "CANCELLED"
  private Instant createdOn;
}
