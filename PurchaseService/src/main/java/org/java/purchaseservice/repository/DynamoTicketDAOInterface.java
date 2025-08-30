package org.java.purchaseservice.repository;

import org.java.purchaseservice.dto.TicketCreationDTO;

public interface DynamoTicketDAOInterface {
		String createTicket(TicketCreationDTO ticket);
//	TicketInfo getTicketInfoById(String id);
}
