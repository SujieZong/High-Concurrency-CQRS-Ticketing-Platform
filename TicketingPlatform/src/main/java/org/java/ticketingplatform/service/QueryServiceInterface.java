package org.java.ticketingplatform.service;

import java.math.BigDecimal;

public interface QueryServiceInterface {
	int countTicketSoldByEvent(String event);
	BigDecimal sumRevenueByVenueAndEvent(String venueId, String eventId);
}
