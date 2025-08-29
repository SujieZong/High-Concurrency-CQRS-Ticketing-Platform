package org.java.ticketingplatform.service;

import org.java.ticketingplatform.dto.TicketCreationDTO;
import org.java.ticketingplatform.dto.TicketRespondDTO;

public interface TicketPurchaseServiceInterface {
	// transfer input data into a Response DTO object and save to Database through DAO and Mapper
	TicketRespondDTO purchaseTicket(TicketCreationDTO ticketCreationDTO);
}
