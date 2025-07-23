package org.java.rabbitcombinedconsumer.repository;

import org.java.rabbitcombinedconsumer.model.TicketInfo;

public interface MySqlTicketDAOInterface {
	void createTicket(TicketInfo ticket);
}
