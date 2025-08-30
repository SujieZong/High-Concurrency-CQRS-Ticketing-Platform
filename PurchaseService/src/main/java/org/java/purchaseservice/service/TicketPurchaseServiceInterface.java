package org.java.purchaseservice.service;

import org.java.purchaseservice.dto.TicketCreationDTO;
import org.java.purchaseservice.dto.TicketRespondDTO;

public interface TicketPurchaseServiceInterface {
	// transfer input data into a Response DTO object and save to Database through DAO and Mapper
	TicketRespondDTO purchaseTicket(TicketCreationDTO ticketCreationDTO);
}
