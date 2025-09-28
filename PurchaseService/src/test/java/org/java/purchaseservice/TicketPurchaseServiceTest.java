package org.java.purchaseservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java.purchaseservice.dto.TicketPurchaseRequestDTO;
import org.java.purchaseservice.dto.TicketRespondDTO;
import org.java.purchaseservice.exception.CreateTicketException;
import org.java.purchaseservice.exception.SeatOccupiedException;
import org.java.purchaseservice.mapper.TicketMapper;
import org.java.purchaseservice.model.TicketInfo;
import org.java.purchaseservice.model.TicketStatus;
import org.java.purchaseservice.outbox.OutboxService;
import org.java.purchaseservice.repository.DynamoTicketDaoInterface;
import org.java.purchaseservice.service.purchase.TicketPurchaseService;
import org.java.purchaseservice.service.redis.SeatOccupiedRedisFacade;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TicketPurchaseServiceTest {

	@Test
	void purchaseTicket_success_emitsOutboxAndReturnsDTO() throws Exception {
		// mocks
		SeatOccupiedRedisFacade seat = mock(SeatOccupiedRedisFacade.class);
		OutboxService outbox = mock(OutboxService.class);
		DynamoTicketDaoInterface dynamo = mock(DynamoTicketDaoInterface.class);
		TicketMapper ticketMapper = mock(TicketMapper.class);

		TicketPurchaseService svc =
				new TicketPurchaseService(ticketMapper, seat, outbox, dynamo);

		// request
		var req = new TicketPurchaseRequestDTO("V1", "E1", 1, "A", "7");

		// mapper: CreationDTO -> entity；entity -> respondDTO
		when(ticketMapper.toEntity(any())).thenAnswer(inv -> {
			var creation = inv.getArgument(0, org.java.purchaseservice.dto.TicketCreationDTO.class);
			var e = new TicketInfo();
			e.setVenueId(creation.getVenueId());
			e.setEventId(creation.getEventId());
			e.setZoneId(creation.getZoneId());
			e.setRow(creation.getRow());
			e.setColumn(creation.getColumn());
			return e;
		});
		when(ticketMapper.toRespondDto(any(TicketInfo.class))).thenAnswer(inv -> {
			TicketInfo t = inv.getArgument(0);
			return new TicketRespondDTO(t.getTicketId(), t.getZoneId(), t.getRow(), t.getColumn(), t.getCreatedOn());
		});

		// outbox 返回一个 id
		when(outbox.saveEvent(eq("ticket.created"), any(), anyString())).thenReturn("outbox-1");

		// act
		TicketRespondDTO resp = svc.purchaseTicket(req);

		// assert —— 占座调用
		verify(seat).tryOccupySeat("E1", "V1", 1, "A", "7");

		// 写库：entity 已被 service 补齐
		verify(dynamo).createTicket(argThat(t -> {
			assertThat(t.getVenueId()).isEqualTo("V1");
			assertThat(t.getEventId()).isEqualTo("E1");
			assertThat(t.getZoneId()).isEqualTo(1);
			assertThat(t.getRow()).isEqualTo("A");
			assertThat(t.getColumn()).isEqualTo("7");
			assertThat(t.getTicketId()).isNotBlank();
			assertThat(t.getCreatedOn()).isNotNull();
			assertThat(t.getStatus()).isEqualTo(TicketStatus.PAID);
			return true;
		}));

		// 写 Outbox：eventType 正确；payloadObj（Map/DTO）里包含关键字段；aggregateId=ticketId
		verify(outbox).saveEvent(eq("ticket.created"), argThat(obj -> {
			String json = (obj instanceof String) ? (String) obj : new ObjectMapper().valueToTree(obj).toString();
			return json.contains("\"eventId\":\"E1\"") && json.contains("\"venueId\":\"V1\"");
		}), anyString()); // 如果你在 service 里把 aggregateId 设为 ticketId，这里可以再用 captor 精确断言

		// 返回 DTO
		assertThat(resp).isNotNull();
		assertThat(resp.getTicketId()).isNotBlank();
		assertThat(resp.getZoneId()).isEqualTo(1);
		assertThat(resp.getRow()).isEqualTo("A");
		assertThat(resp.getColumn()).isEqualTo("7");
		assertThat(resp.getCreatedOn()).isNotNull();
	}

	@Test
	void purchaseTicket_whenSeatAlreadyOccupied_throws_andNoDynamoNoOutbox() {
		SeatOccupiedRedisFacade seat = mock(SeatOccupiedRedisFacade.class);
		OutboxService outbox = mock(OutboxService.class);
		DynamoTicketDaoInterface dynamo = mock(DynamoTicketDaoInterface.class);
		// ObjectMapper mapper = new ObjectMapper();
		TicketMapper ticketMapper = mock(TicketMapper.class);

		TicketPurchaseService svc =
				new TicketPurchaseService(ticketMapper, seat, outbox, dynamo);

		var req = new TicketPurchaseRequestDTO("V1", "E1", 1, "A", "7");

		doThrow(new SeatOccupiedException("occupied"))
				.when(seat).tryOccupySeat("E1", "V1", 1, "A", "7");

		assertThatThrownBy(() -> svc.purchaseTicket(req)).isInstanceOf(SeatOccupiedException.class);

		verify(dynamo, never()).createTicket(any());
		verify(outbox, never()).saveEvent(anyString(), any(), anyString());
	}

	@Test
	void purchaseTicket_whenDynamoFails_releaseSeat_andThrowCreateTicketException() {
		SeatOccupiedRedisFacade seat = mock(SeatOccupiedRedisFacade.class);
		OutboxService outbox = mock(OutboxService.class);
		DynamoTicketDaoInterface dynamo = mock(DynamoTicketDaoInterface.class);
		// ObjectMapper mapper = new ObjectMapper();
		TicketMapper ticketMapper = mock(TicketMapper.class);

		when(ticketMapper.toEntity(any())).thenReturn(new TicketInfo());

		TicketPurchaseService svc =
				new TicketPurchaseService(ticketMapper, seat, outbox, dynamo);

		var req = new TicketPurchaseRequestDTO("V1", "E1", 1, "A", "7");

		doThrow(new RuntimeException("dynamo down")).when(dynamo).createTicket(any());

		assertThatThrownBy(() -> svc.purchaseTicket(req))
				.isInstanceOf(CreateTicketException.class);

		verify(seat).releaseSeat("E1", "V1", 1, "A", "7");
		verify(outbox, never()).saveEvent(anyString(), any(), anyString());
	}

	@Test
	void purchaseTicket_whenOutboxFails_releaseSeat_andThrowCreateTicketException() {
		SeatOccupiedRedisFacade seat = mock(SeatOccupiedRedisFacade.class);
		OutboxService outbox = mock(OutboxService.class);
		DynamoTicketDaoInterface dynamo = mock(DynamoTicketDaoInterface.class);
		TicketMapper ticketMapper = mock(TicketMapper.class);

		when(ticketMapper.toEntity(any())).thenReturn(new TicketInfo());

		TicketPurchaseService svc =
				new TicketPurchaseService(ticketMapper, seat, outbox, dynamo);

		var req = new TicketPurchaseRequestDTO("V1", "E1", 1, "A", "7");


		doThrow(new RuntimeException("outbox down"))
				.when(outbox).saveEvent(anyString(), any(), anyString());

		assertThatThrownBy(() -> svc.purchaseTicket(req))
				.isInstanceOf(CreateTicketException.class);

		verify(seat).releaseSeat("E1", "V1", 1, "A", "7");
	}
}