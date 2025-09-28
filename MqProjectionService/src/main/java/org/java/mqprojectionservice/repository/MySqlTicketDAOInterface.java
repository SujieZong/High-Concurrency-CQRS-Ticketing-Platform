package org.java.mqprojectionservice.repository;

import org.java.mqprojectionservice.model.TicketInfo;

public interface MySqlTicketDAOInterface {
	void createTicket(TicketInfo ticket);
}
