package org.java.rabbitcombinedconsumer.service.rabbitmq;

import org.java.rabbitcombinedconsumer.model.TicketInfo;
import org.java.rabbitcombinedconsumer.model.TicketStatus;
import org.java.rabbitcombinedconsumer.repository.mysql.MySqlTicketDao;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RabbitMySqlConsumerTest {

	@Test
	void createTicket_success_executesInsert_withExpectedParams() {
		// arrange
		JdbcTemplate jdbc = mock(JdbcTemplate.class);
		when(jdbc.update(anyString(),
				any(), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(1);

		MySqlTicketDao dao = new MySqlTicketDao(jdbc);

		TicketInfo t = new TicketInfo();
		t.setTicketId("t-100");
		t.setVenueId("v-1");
		t.setEventId("e-1");
		t.setZoneId(12);
		t.setRow("A");
		t.setColumn("10");
		t.setStatus(TicketStatus.PAID);
		Instant created = Instant.parse("2025-08-27T12:34:56Z");
		t.setCreatedOn(created);

		// act
		dao.createTicket(t);

		// assert
		ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);

		// 只捕获 SQL；对 varargs 逐个匹配
		verify(jdbc, times(1)).update(
				sqlCap.capture(),
				eq("t-100"),           // 0: ticket_id
				eq("v-1"),             // 1: venue_id
				eq("e-1"),             // 2: event_id
				eq(12),                // 3: zone_id
				eq("A"),               // 4: row_label
				eq("10"),              // 5: col_label
				eq("PAID"),            // 6: status.name()
				eq(Timestamp.from(created)) // 7: created_on
		);

		String sql = sqlCap.getValue();
		assertNotNull(sql);
		assertTrue(sql.toUpperCase().contains("INSERT INTO TICKET"), "应为插入 ticket 的 SQL");
	}

	@Test
	void createTicket_duplicateKey_doesNotThrow_andSkips() {
		// arrange
		JdbcTemplate jdbc = mock(JdbcTemplate.class);
		when(jdbc.update(anyString(),
				any(), any(), any(), any(), any(), any(), any(), any()))
				.thenThrow(new DuplicateKeyException("dup"));

		MySqlTicketDao dao = new MySqlTicketDao(jdbc);

		TicketInfo t = new TicketInfo();
		t.setTicketId("t-dup");
		t.setVenueId("v-1");
		t.setEventId("e-1");
		t.setZoneId(1);
		t.setRow("B");
		t.setColumn("2");
		t.setStatus(TicketStatus.PAID);
		t.setCreatedOn(Instant.parse("2025-08-27T00:00:00Z"));

		assertDoesNotThrow(() -> dao.createTicket(t));

		verify(jdbc, times(1)).update(
				anyString(),
				any(), any(), any(), any(), any(), any(), any(), any()
		);
		verifyNoMoreInteractions(jdbc);
	}
}