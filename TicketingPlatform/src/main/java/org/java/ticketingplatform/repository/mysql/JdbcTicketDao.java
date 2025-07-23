package org.java.ticketingplatform.repository.mysql;

import lombok.extern.slf4j.Slf4j;
import org.java.ticketingplatform.repository.MySqlTicketDAOInterface;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Slf4j
@Repository
public class JdbcTicketDao implements MySqlTicketDAOInterface {

	private final JdbcTemplate jdbcTemplate;

	public JdbcTicketDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

//	@Override
//	public void createTicket(TicketInfo ticketInfo) {
//		String sql = """
//				  INSERT INTO ticket(
//				    ticket_id, venue_id, event_id,
//				    zone_id, row_label, col_label,
//				    created_on
//				  ) VALUES(?,?,?,?,?,?,?)
//				""";
//		try {
//			jdbcTemplate.update(sql, ticketInfo.getTicketId(), ticketInfo.getVenueId(), ticketInfo.getEventId(), ticketInfo.getZoneId(), ticketInfo.getRow(), ticketInfo.getColumn(), Timestamp.from(ticketInfo.getCreatedOn()));
//			log.debug("[JdbcTicketDao] Successfully persisted ticket with id={}", ticketInfo.getTicketId());
//		} catch (DuplicateKeyException e) {
//			log.warn("[JdbcTicketDao] ticketId = {}, exists skip", ticketInfo.getTicketId());
//		}
//	}

	@Override
	public int countTicketSold(String eventId) {
		log.debug("[JdbcTicketDao][countTicketSold] SQL COUNT for eventId={}", eventId);
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket WHERE event_id = ?",
				Integer.class, eventId);

		int result = (count == null ? 0 : count);
		log.debug("[JdbcTicketDao][countTicketSold] result={}", result);

		return result;
	}

	@Override
	public BigDecimal sumRevenueByVenueEvent(String venueId, String eventId) {
		log.debug("[JdbcTicketDao][sumRevenue] SQL SUM for venueId={},eventId={}", venueId, eventId);
		BigDecimal sum = jdbcTemplate.queryForObject("""
				  SELECT SUM(z.ticket_price)
				    FROM ticket t
				    JOIN zone z ON t.venue_id=z.venue_id AND t.zone_id=z.zone_id
				   WHERE t.venue_id = ? AND t.event_id = ?
				""", BigDecimal.class, venueId, eventId);

		BigDecimal result = (sum == null ? BigDecimal.ZERO : sum);
		log.debug("[JdbcTicketDao][sumRevenue] result={}", result);
		return sum;
	}
}
