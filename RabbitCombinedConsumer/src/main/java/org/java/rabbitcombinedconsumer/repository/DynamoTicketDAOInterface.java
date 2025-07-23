package org.java.rabbitcombinedconsumer.repository;

import org.java.rabbitcombinedconsumer.model.TicketCreation;

public interface DynamoTicketDAOInterface {
	String createTicket(TicketCreation ticket);
}
