package org.java.ticketingplatform.service;

import org.java.ticketingplatform.dto.TicketInfoDTO;
import org.java.ticketingplatform.model.TicketInfo;

import java.math.BigDecimal;

public interface QueryServiceInterface {
	TicketInfoDTO getTicket(String ticketId);

	int countTicketSoldByEvent(String eventId);

	BigDecimal sumRevenueByVenueAndEvent(String venueId, String eventId);
}
