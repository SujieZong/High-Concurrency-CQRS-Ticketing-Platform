package org.java.mqprojectionservice.repository.mysql;

import lombok.extern.slf4j.Slf4j;
import org.java.mqprojectionservice.model.TicketInfo;
import org.java.mqprojectionservice.model.TicketStatus;
import org.java.mqprojectionservice.repository.MySqlTicketDAOInterface;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Slf4j
@Repository
public class MySqlTicketDao implements MySqlTicketDAOInterface {

	private final JdbcTemplate jdbcTemplate;

	public MySqlTicketDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void createTicket(TicketInfo ticketInfo) {
		String sql = """
				  INSERT INTO ticket(
				    ticket_id, venue_id, event_id,
				    zone_id, row_label, col_label, status,
				    created_on
				  ) VALUES(?,?,?,?,?,?,?,?)
				  ON DUPLICATE KEY UPDATE
				    status = VALUES(status)
				""";
		try {
			jdbcTemplate.update(
					sql,
					ticketInfo.getTicketId(), ticketInfo.getVenueId(), ticketInfo.getEventId(),
					ticketInfo.getZoneId(), ticketInfo.getRow(), ticketInfo.getColumn(),
					(ticketInfo.getStatus() == null ? TicketStatus.PENDING_PAYMENT : ticketInfo.getStatus()).name(),
					Timestamp.from(ticketInfo.getCreatedOn())
			);
			log.debug("[MySqlTicketDao] Successfully persisted ticket with id={}", ticketInfo.getTicketId());
		} catch (DuplicateKeyException e) {
			log.warn("[MySqlTicketDao] ticketId = {}, exists skip", ticketInfo.getTicketId());
		}
	}
}
