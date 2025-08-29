package org.java.ticketingplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java.ticketingplatform.dto.TicketCreationDTO;
import org.java.ticketingplatform.dto.TicketRespondDTO;
import org.java.ticketingplatform.exception.CreateTicketException;
import org.java.ticketingplatform.exception.SeatOccupiedException;
import org.java.ticketingplatform.repository.DynamoTicketDAOInterface;
import org.java.ticketingplatform.service.outbox.OutboxService;
import org.java.ticketingplatform.service.purchase.TicketPurchaseService;
import org.java.ticketingplatform.service.redis.SeatOccupiedRedisFacade;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class TicketPurchaseServiceTest {

	@Test
	void purchaseTicket_success_emitsOutboxAndReturnsDTO() throws Exception {
		// Arrange
		SeatOccupiedRedisFacade seatFacade = mock(SeatOccupiedRedisFacade.class);
		OutboxService outbox = mock(OutboxService.class);
		DynamoTicketDAOInterface dynamo = mock(DynamoTicketDAOInterface.class);
		ObjectMapper mapper = new ObjectMapper();

		TicketPurchaseService svc = new TicketPurchaseService(seatFacade, outbox, mapper, dynamo);

		// Mock
		TicketCreationDTO dto = mock(TicketCreationDTO.class);
		when(dto.getEventId()).thenReturn("E1");
		when(dto.getVenueId()).thenReturn("V1");
		when(dto.getZoneId()).thenReturn(1);
		when(dto.getRow()).thenReturn("A");
		when(dto.getColumn()).thenReturn("7");
		when(dto.getStatus()).thenReturn(null);


		ArgumentCaptor<TicketCreationDTO> dynamoCap = ArgumentCaptor.forClass(TicketCreationDTO.class);
		ArgumentCaptor<String> typeCap = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> payloadCap = ArgumentCaptor.forClass(String.class);

		// Act
		TicketRespondDTO resp = svc.purchaseTicket(dto);

		// Assert
		// 1) Try to occupy a seat
		verify(seatFacade, times(1)).tryOccupySeat("E1", "V1", 1, "A", "7");

		// 2) Outbox Write， ticket.created
		verify(outbox, times(1)).saveEvent(typeCap.capture(), payloadCap.capture());

		verify(dynamo, times(1)).createTicket(dynamoCap.capture());
		TicketCreationDTO written = dynamoCap.getValue();
		assertThat(written.getEventId()).isEqualTo("E1");
		assertThat(written.getVenueId()).isEqualTo("V1");
		assertThat(written.getZoneId()).isEqualTo(1);
		assertThat(written.getRow()).isEqualTo("A");
		assertThat(written.getColumn()).isEqualTo("7");
		assertThat(written.getStatus()).isEqualTo("PAID"); // 默认值生效
		assertThat(written.getId()).isNotBlank();
		assertThat(written.getCreatedOn()).isNotNull();

		// 3) 再写 Outbox
		verify(outbox, times(1)).saveEvent(typeCap.capture(), payloadCap.capture());
		assertThat(typeCap.getValue()).isEqualTo("ticket.created");
		String json = payloadCap.getValue();
		assertThat(json).contains("\"status\":\"PAID\"");
		assertThat(json).contains("\"eventId\":\"E1\"");
		assertThat(json).contains("\"venueId\":\"V1\"");
		assertThat(json).contains("\"zoneId\":1");
		assertThat(json).contains("\"row\":\"A\"");
		assertThat(json).contains("\"column\":\"7\"");
		assertThat(json).contains("\"ticketId\"");

		// 4) 返回 DTO
		assertThat(resp).isNotNull();
		assertThat(resp.getZoneId()).isEqualTo(1);
		assertThat(resp.getRow()).isEqualTo("A");
		assertThat(resp.getColumn()).isEqualTo("7");
		assertThat(resp.getCreatedOn()).isNotNull();
		assertThat(resp.getTicketId()).isNotBlank();
	}

	@Test
	void purchaseTicket_whenSeatAlreadyOccupied_throws_andNoDynamoNoOutbox() {
		SeatOccupiedRedisFacade seatFacade = mock(SeatOccupiedRedisFacade.class);
		OutboxService outbox = mock(OutboxService.class);
		DynamoTicketDAOInterface dynamo = mock(DynamoTicketDAOInterface.class);
		ObjectMapper mapper = new ObjectMapper();

		TicketPurchaseService svc = new TicketPurchaseService(seatFacade, outbox, mapper, dynamo);

		TicketCreationDTO dto = mock(TicketCreationDTO.class);
		when(dto.getEventId()).thenReturn("E1");
		when(dto.getVenueId()).thenReturn("V1");
		when(dto.getZoneId()).thenReturn(1);
		when(dto.getRow()).thenReturn("A");
		when(dto.getColumn()).thenReturn("7");

		doThrow(new SeatOccupiedException("occupied"))
				.when(seatFacade).tryOccupySeat("E1", "V1", 1, "A", "7");

		assertThatThrownBy(() -> svc.purchaseTicket(dto))
				.isInstanceOf(SeatOccupiedException.class);

		verify(dynamo, never()).createTicket(any());
		verify(outbox, never()).saveEvent(anyString(), anyString());
	}


	@Test
	void purchaseTicket_whenDynamoFails_releaseSeat_andThrowCreateTicketException() {
		SeatOccupiedRedisFacade seatFacade = mock(SeatOccupiedRedisFacade.class);
		OutboxService outbox = mock(OutboxService.class);
		DynamoTicketDAOInterface dynamo = mock(DynamoTicketDAOInterface.class);
		ObjectMapper mapper = new ObjectMapper();

		TicketPurchaseService svc = new TicketPurchaseService(seatFacade, outbox, mapper, dynamo);

		TicketCreationDTO dto = mock(TicketCreationDTO.class);
		when(dto.getEventId()).thenReturn("E1");
		when(dto.getVenueId()).thenReturn("V1");
		when(dto.getZoneId()).thenReturn(1);
		when(dto.getRow()).thenReturn("A");
		when(dto.getColumn()).thenReturn("7");

		doThrow(new RuntimeException("dynamo down"))
				.when(dynamo).createTicket(any());

		assertThatThrownBy(() -> svc.purchaseTicket(dto))
				.isInstanceOf(CreateTicketException.class);

		// 释放座位被调用
		verify(seatFacade, times(1)).releaseSeat("E1", "V1", 1, "A", "7");
		// 不应写 Outbox
		verify(outbox, never()).saveEvent(anyString(), anyString());
	}

	@Test
	void purchaseTicket_whenOutboxFails_releaseSeat_andThrowCreateTicketException() throws Exception {
		SeatOccupiedRedisFacade seatFacade = mock(SeatOccupiedRedisFacade.class);
		OutboxService outbox = mock(OutboxService.class);
		DynamoTicketDAOInterface dynamo = mock(DynamoTicketDAOInterface.class);
		ObjectMapper mapper = new ObjectMapper();

		TicketPurchaseService svc = new TicketPurchaseService(seatFacade, outbox, mapper, dynamo);

		TicketCreationDTO dto = mock(TicketCreationDTO.class);
		when(dto.getEventId()).thenReturn("E1");
		when(dto.getVenueId()).thenReturn("V1");
		when(dto.getZoneId()).thenReturn(1);
		when(dto.getRow()).thenReturn("A");
		when(dto.getColumn()).thenReturn("7");

		// Dynamo 成功
		when(dynamo.createTicket(any())).thenReturn("any-id");
		// Outbox 抛错
		doThrow(new RuntimeException("outbox down"))
				.when(outbox).saveEvent(anyString(), anyString());

		assertThatThrownBy(() -> svc.purchaseTicket(dto))
				.isInstanceOf(CreateTicketException.class);

		// 释放座位被调用
		verify(seatFacade, times(1)).releaseSeat("E1", "V1", 1, "A", "7");
	}
}

