package org.java.purchaseservice.repository;

import org.java.purchaseservice.model.TicketInfo;


public interface DynamoTicketDaoInterface {
	void createTicket(TicketInfo ticket);
}
