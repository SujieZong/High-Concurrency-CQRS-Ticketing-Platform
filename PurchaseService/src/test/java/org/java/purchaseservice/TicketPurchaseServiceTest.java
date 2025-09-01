package org.java.purchaseservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java.purchaseservice.dto.TicketPurchaseRequestDTO;
import org.java.purchaseservice.dto.TicketRespondDTO;
import org.java.purchaseservice.exception.CreateTicketException;
import org.java.purchaseservice.exception.SeatOccupiedException;
import org.java.purchaseservice.mapper.TicketMapper;
import org.java.purchaseservice.model.TicketInfo;
import org.java.purchaseservice.model.TicketStatus;
import org.java.purchaseservice.repository.DynamoTicketDaoInterface;
import org.java.purchaseservice.service.outbox.OutboxService;
import org.java.purchaseservice.service.purchase.TicketPurchaseService;
import org.java.purchaseservice.service.redis.SeatOccupiedRedisFacade;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class TicketPurchaseServiceTest {

	@Test
	void purchaseTicket_success_emitsOutboxAndReturnsDTO() throws Exception {
		// Mocks
		SeatOccupiedRedisFacade seatFacade = mock(SeatOccupiedRedisFacade.class);
		OutboxService outbox = mock(OutboxService.class);
		DynamoTicketDaoInterface dynamo = mock(DynamoTicketDaoInterface.class);
		ObjectMapper mapper = new ObjectMapper();
		TicketMapper ticketMapper = mock(TicketMapper.class);

		// Service with mapper
		TicketPurchaseService svc = new TicketPurchaseService(ticketMapper, seatFacade, outbox, mapper, dynamo);

		// Request
		TicketPurchaseRequestDTO req = new TicketPurchaseRequestDTO("V1", "E1", 1, "A", "7");

		// Mapper behavior: CreationDTO -> Entity, Entity -> RespondDTO
		TicketInfo entity = new TicketInfo();
		// 注意：ticketId/status/createdOn 由 Service 生成后 set 到 entity（测试里先不设）
		entity.setVenueId("V1");
		entity.setEventId("E1");
		entity.setZoneId(1);
		entity.setRow("A");
		entity.setColumn("7");

		when(ticketMapper.toEntity(any())).thenReturn(entity);
		when(ticketMapper.toRespondDto(any(TicketInfo.class))).thenAnswer(inv -> {
			TicketInfo t = inv.getArgument(0);
			return new TicketRespondDTO(t.getTicketId(), t.getZoneId(), t.getRow(), t.getColumn(), t.getCreatedOn());
		});

		// Act
		TicketRespondDTO resp = svc.purchaseTicket(req);

		// Assert —— Redis占座
		verify(seatFacade, times(1)).tryOccupySeat("E1", "V1", 1, "A", "7");

		// Assert —— DAO 收到实体（已由 Service 补齐 id/status/createdOn）
		verify(dynamo, times(1)).createTicket(argThat(t -> {
			assertThat(t.getVenueId()).isEqualTo("V1");
			assertThat(t.getEventId()).isEqualTo("E1");
			assertThat(t.getZoneId()).isEqualTo(1);
			assertThat(t.getRow()).isEqualTo("A");
			assertThat(t.getColumn()).isEqualTo("7");
			assertThat(t.getTicketId()).isNotBlank();
			assertThat(t.getCreatedOn()).isNotNull();
			assertThat(t.getStatus()).isIn(TicketStatus.PAID, TicketStatus.PAID); // 看你默认是哪一个
			return true;
		}));

		// Assert —— Outbox 被写入一次
		verify(outbox, times(1)).saveEvent(eq("ticket.created"), contains("\"eventId\":\"E1\""));

		// Assert —— 返回 DTO
		assertThat(resp).isNotNull();
		assertThat(resp.getTicketId()).isNotBlank();
		assertThat(resp.getZoneId()).isEqualTo(1);
		assertThat(resp.getRow()).isEqualTo("A");
		assertThat(resp.getColumn()).isEqualTo("7");
		assertThat(resp.getCreatedOn()).isNotNull();
	}

	@Test
	void purchaseTicket_whenSeatAlreadyOccupied_throws_andNoDynamoNoOutbox() {
		SeatOccupiedRedisFacade seatFacade = mock(SeatOccupiedRedisFacade.class);
		OutboxService outbox = mock(OutboxService.class);
		DynamoTicketDaoInterface dynamo = mock(DynamoTicketDaoInterface.class);
		ObjectMapper mapper = new ObjectMapper();
		TicketMapper ticketMapper = mock(TicketMapper.class);

		TicketPurchaseService svc = new TicketPurchaseService(ticketMapper, seatFacade, outbox, mapper, dynamo);

		TicketPurchaseRequestDTO req = new TicketPurchaseRequestDTO("V1", "E1", 1, "A", "7");

		doThrow(new SeatOccupiedException("occupied")).when(seatFacade).tryOccupySeat("E1", "V1", 1, "A", "7");

		assertThatThrownBy(() -> svc.purchaseTicket(req)).isInstanceOf(SeatOccupiedException.class);

		verify(dynamo, never()).createTicket(any());
		verify(outbox, never()).saveEvent(anyString(), anyString());
	}

	@Test
	void purchaseTicket_whenDynamoFails_releaseSeat_andThrowCreateTicketException() {
		SeatOccupiedRedisFacade seatFacade = mock(SeatOccupiedRedisFacade.class);
		OutboxService outbox = mock(OutboxService.class);
		DynamoTicketDaoInterface dynamo = mock(DynamoTicketDaoInterface.class);
		ObjectMapper mapper = new ObjectMapper();
		TicketMapper ticketMapper = mock(TicketMapper.class);

		// 为了走到写库，这里需要让 mapper 返回一个可用实体
		when(ticketMapper.toEntity(any())).thenReturn(new TicketInfo());

		TicketPurchaseService svc = new TicketPurchaseService(ticketMapper, seatFacade, outbox, mapper, dynamo);

		TicketPurchaseRequestDTO req = new TicketPurchaseRequestDTO("V1", "E1", 1, "A", "7");

		doThrow(new RuntimeException("dynamo down")).when(dynamo).createTicket(any());

		assertThatThrownBy(() -> svc.purchaseTicket(req)).isInstanceOf(CreateTicketException.class);

		verify(seatFacade, times(1)).releaseSeat("E1", "V1", 1, "A", "7");
		verify(outbox, never()).saveEvent(anyString(), anyString());
	}

	@Test
	void purchaseTicket_whenOutboxFails_releaseSeat_andThrowCreateTicketException() {
		SeatOccupiedRedisFacade seatFacade = mock(SeatOccupiedRedisFacade.class);
		OutboxService outbox = mock(OutboxService.class);
		DynamoTicketDaoInterface dynamo = mock(DynamoTicketDaoInterface.class);
		ObjectMapper mapper = new ObjectMapper();
		TicketMapper ticketMapper = mock(TicketMapper.class);

		when(ticketMapper.toEntity(any())).thenReturn(new TicketInfo());

		TicketPurchaseService svc = new TicketPurchaseService(ticketMapper, seatFacade, outbox, mapper, dynamo);

		TicketPurchaseRequestDTO req = new TicketPurchaseRequestDTO("V1", "E1", 1, "A", "7");

		// Dynamo 成功（void）
		// Outbox 抛错
		doThrow(new RuntimeException("outbox down")).when(outbox).saveEvent(anyString(), anyString());

		assertThatThrownBy(() -> svc.purchaseTicket(req)).isInstanceOf(CreateTicketException.class);

		verify(seatFacade, times(1)).releaseSeat("E1", "V1", 1, "A", "7");
	}
}