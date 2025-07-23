package org.java.ticketingplatform.repository;

import org.java.ticketingplatform.model.TicketInfo;

public interface DynamoTicketDAOInterface {
//		String createTicket(TicketCreation ticket);
	TicketInfo getTicketInfoById(String id);
}
