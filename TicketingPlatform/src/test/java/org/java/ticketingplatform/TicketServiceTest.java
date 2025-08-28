package org.java.ticketingplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java.ticketingplatform.dto.TicketCreationDTO;
import org.java.ticketingplatform.dto.TicketRespondDTO;
import org.java.ticketingplatform.exception.SeatOccupiedException;
import org.java.ticketingplatform.service.SeatOccupiedRedisFacade;
import org.java.ticketingplatform.service.TicketService;
import org.java.ticketingplatform.service.outbox.OutboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TicketServiceTest {

	@Test
	void purchaseTicket_success_emitsOutboxAndReturnsDTO() throws Exception {
		// Arrange
		SeatOccupiedRedisFacade seatFacade = mock(SeatOccupiedRedisFacade.class);
		OutboxService outbox = mock(OutboxService.class);
		ObjectMapper mapper = new ObjectMapper();

		TicketService svc = new TicketService(seatFacade, outbox, mapper);

		// Mock
		TicketCreationDTO dto = mock(TicketCreationDTO.class);
		when(dto.getEventId()).thenReturn("E1");
		when(dto.getVenueId()).thenReturn("V1");
		when(dto.getZoneId()).thenReturn(1);       // int
		when(dto.getRow()).thenReturn("A");        // String
		when(dto.getColumn()).thenReturn("7");     // String


		ArgumentCaptor<String> typeCap = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> payloadCap = ArgumentCaptor.forClass(String.class);

		// Act
		TicketRespondDTO resp = svc.purchaseTicket(dto);

		// Assert
		// 1) Try to occupy a seat
		verify(seatFacade, times(1)).tryOccupySeat("E1", "V1", 1, "A", "7");

		// 2) Outbox Write， ticket.created
		verify(outbox, times(1)).saveEvent(typeCap.capture(), payloadCap.capture());
		assertThat(typeCap.getValue()).isEqualTo("ticket.created");

		// 3) payload Keywords
		String json = payloadCap.getValue();
		assertThat(json).contains("\"status\":\"PAID\"");
		assertThat(json).contains("\"eventId\":\"E1\"");
		assertThat(json).contains("\"venueId\":\"V1\"");
		assertThat(json).contains("\"zoneId\":1");        // Int
		assertThat(json).contains("\"row\":\"A\"");       // String
		assertThat(json).contains("\"column\":\"7\"");    // String
		assertThat(json).contains("\"ticketId\"");        // UUID

		// 4) DTO Legit Check
		assertThat(resp).isNotNull();
		assertThat(resp.getZoneId()).isEqualTo(1);
		assertThat(resp.getRow()).isEqualTo("A");
		assertThat(resp.getColumn()).isEqualTo("7");
		assertThat(resp.getCreatedOn()).isNotNull();
		assertThat(resp.getTicketId()).isNotBlank();
	}

	@Test
	void purchaseTicket_whenSeatAlreadyOccupied_throwsAndNoOutbox() {
		// Arrange
		SeatOccupiedRedisFacade seatFacade = mock(SeatOccupiedRedisFacade.class);
		OutboxService outbox = mock(OutboxService.class);
		ObjectMapper mapper = new ObjectMapper();
		TicketService svc = new TicketService(seatFacade, outbox, mapper);

		TicketCreationDTO dto = mock(TicketCreationDTO.class);
		when(dto.getEventId()).thenReturn("E1");
		when(dto.getVenueId()).thenReturn("V1");
		when(dto.getZoneId()).thenReturn(1);
		when(dto.getRow()).thenReturn("A");
		when(dto.getColumn()).thenReturn("7");

		doThrow(new SeatOccupiedException("occupied"))
				.when(seatFacade)
				.tryOccupySeat("E1", "V1", 1, "A", "7");

		// Act + Assert
		assertThatThrownBy(() -> svc.purchaseTicket(dto))
				.isInstanceOf(SeatOccupiedException.class);

		//Avoid Outbox
		verify(outbox, never()).saveEvent(anyString(), anyString());
	}
}
