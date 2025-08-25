package org.java.ticketingplatform.repository;

import org.java.ticketingplatform.model.TicketInfo;

public interface DynamoTicketDAOInterface {
//		String createTicket(TicketCreationDTO ticket);
	TicketInfo getTicketInfoById(String id);
}
