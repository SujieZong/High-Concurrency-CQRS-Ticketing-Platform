package org.java.rabbitcombinedconsumer.repository.mysql;

import lombok.extern.slf4j.Slf4j;
import org.java.rabbitcombinedconsumer.model.TicketInfo;
import org.java.rabbitcombinedconsumer.repository.MySqlTicketDAOInterface;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Slf4j
@Repository
public class JdbcTicketDao implements MySqlTicketDAOInterface {

	private final JdbcTemplate jdbcTemplate;

	public JdbcTicketDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void createTicket(TicketInfo ticketInfo) {
		String sql = """
				  INSERT INTO ticket(
				    ticket_id, venue_id, event_id,
				    zone_id, row_label, col_label,
				    created_on
				  ) VALUES(?,?,?,?,?,?,?)
				""";
		try {
			jdbcTemplate.update(sql, ticketInfo.getTicketId(), ticketInfo.getVenueId(), ticketInfo.getEventId(), ticketInfo.getZoneId(), ticketInfo.getRow(), ticketInfo.getColumn(), Timestamp.from(ticketInfo.getCreatedOn()));
			log.debug("[JdbcTicketDao] Successfully persisted ticket with id={}", ticketInfo.getTicketId());
		} catch (DuplicateKeyException e) {
			log.warn("[JdbcTicketDao] ticketId = {}, exists skip", ticketInfo.getTicketId());
		}
	}
}
