package org.java.ticketingplatform.service;

import org.java.ticketingplatform.dto.TicketRespondDTO;
import org.java.ticketingplatform.model.TicketInfo;

public interface TicketServiceInterface {
	// transfer input data into a Response DTO object and save to Database through DAO and Mapper
	TicketRespondDTO createTicket(String venueId, String eventId, int zoneId, String row, String column);

	// call get from DAO
	TicketInfo getTicket(String ticketId);
}
