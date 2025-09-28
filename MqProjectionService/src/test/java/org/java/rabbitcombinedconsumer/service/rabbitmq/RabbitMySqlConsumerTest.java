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
		JdbcTemplate jdbc = mock(JdbcTemplate.class);
		when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
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

		dao.createTicket(t);

		ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object[]> argsCap = ArgumentCaptor.forClass(Object[].class);
		verify(jdbc, times(1)).update(sqlCap.capture(), argsCap.capture());

		String sql = sqlCap.getValue();
		assertNotNull(sql);
		assertTrue(sql.toUpperCase().contains("INSERT INTO TICKET"));

		Object[] args = argsCap.getValue();
		assertNotNull(args);
		assertEquals(8, args.length, "insert 参数个数应为 8");

		assertEquals("t-100", args[0]);     // ticket_id
		assertEquals("v-1", args[1]);     // venue_id
		assertEquals("e-1", args[2]);     // event_id
		assertEquals(12, args[3]);     // zone_id
		assertEquals("A", args[4]);     // row_label
		assertEquals("10", args[5]);     // col_label
		assertEquals("PAID", args[6]);     // status.name()
		assertEquals(Timestamp.from(created), args[7]); // created_on
	}

	@Test
	void createTicket_duplicateKey_doesNotThrow_andSkips() {
		// arrange
		JdbcTemplate jdbc = mock(JdbcTemplate.class);
		when(jdbc.update(anyString(), any(Object[].class)))
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

		// act & assert（不抛异常即可）
		assertDoesNotThrow(() -> dao.createTicket(t));

		// 依然只调用了一次 update
		verify(jdbc, times(1)).update(anyString(), any(Object[].class));
	}
}
