package org.java.ticketingplatform.service.query;

import lombok.extern.slf4j.Slf4j;
import org.java.ticketingplatform.repository.MySqlTicketDAOInterface;
import org.java.ticketingplatform.service.QueryServiceInterface;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class QueryService implements QueryServiceInterface {
	private final MySqlTicketDAOInterface sqlDao;
	public QueryService(MySqlTicketDAOInterface sqlDao) {
		this.sqlDao = sqlDao;
	}

	@Override
	public int countTicketSoldByEvent(String eventId) {
		log.debug("[QueryService][countTicketSoldByEvent] start for eventId={}", eventId);
		int count = sqlDao.countTicketSold(eventId);
		log.debug("[QueryService][countTicketSoldByEvent] result={} for eventId={}", count, eventId);
		return count;
	}

	@Override
	public BigDecimal sumRevenueByVenueAndEvent(String venueId, String eventId) {
		log.debug("[QueryService][sumRevenueByVenueAndEvent] start venueId={},eventId={}", venueId, eventId);
		BigDecimal revenue = sqlDao.sumRevenueByVenueEvent(venueId, eventId);
		log.debug("[QueryService][sumRevenueByVenueAndEvent] result={} for venueId={},eventId={}",
				revenue, venueId, eventId);
		return revenue;
	}
}
