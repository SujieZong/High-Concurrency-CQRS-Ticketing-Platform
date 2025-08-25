package org.java.ticketingplatform.service;

import org.java.ticketingplatform.dto.TicketCreationDTO;
import org.java.ticketingplatform.dto.TicketInfoDTO;
import org.java.ticketingplatform.dto.TicketRespondDTO;
import org.java.ticketingplatform.model.TicketInfo;

public interface TicketServiceInterface {
	// transfer input data into a Response DTO object and save to Database through DAO and Mapper
	TicketRespondDTO purchaseTicket(TicketCreationDTO ticketCreationDTO);
}
