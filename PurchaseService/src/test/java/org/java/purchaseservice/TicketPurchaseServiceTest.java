package org.java.purchaseservice;

import org.java.purchaseservice.dto.TicketPurchaseRequestDTO;
import org.java.purchaseservice.dto.TicketRespondDTO;
import org.java.purchaseservice.event.TicketCreatedEvent;
import org.java.purchaseservice.exception.CreateTicketException;
import org.java.purchaseservice.exception.SeatOccupiedException;
import org.java.purchaseservice.mapper.TicketMapper;
import org.java.purchaseservice.model.TicketInfo;
import org.java.purchaseservice.model.TicketStatus;
import org.java.purchaseservice.service.purchase.TicketPurchaseService;
import org.java.purchaseservice.service.redis.SeatOccupiedRedisFacade;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TicketPurchaseServiceTest {

	@Test
	void purchaseTicket_success_publishesEventAndReturnsDTO() {
		SeatOccupiedRedisFacade seat = mock(SeatOccupiedRedisFacade.class);
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		TicketMapper ticketMapper = mock(TicketMapper.class);

		TicketPurchaseService svc = new TicketPurchaseService(ticketMapper, seat, eventPublisher);

		var req = new TicketPurchaseRequestDTO("V1", "E1", 1, "A", "7");

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

		TicketRespondDTO resp = svc.purchaseTicket(req);

		verify(seat).tryOccupySeat("E1", "V1", 1, "A", "7");

		ArgumentCaptor<TicketCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TicketCreatedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());

		TicketCreatedEvent publishedEvent = eventCaptor.getValue();
		assertThat(publishedEvent.getVenueId()).isEqualTo("V1");
		assertThat(publishedEvent.getEventId()).isEqualTo("E1");
		assertThat(publishedEvent.getZoneId()).isEqualTo(1);
		assertThat(publishedEvent.getRow()).isEqualTo("A");
		assertThat(publishedEvent.getColumn()).isEqualTo("7");
		assertThat(publishedEvent.getTicketId()).isNotBlank();
		assertThat(publishedEvent.getStatus()).isEqualTo(TicketStatus.PAID);
		assertThat(publishedEvent.getCreatedOn()).isNotNull();

		// DTO
		assertThat(resp).isNotNull();
		assertThat(resp.getTicketId()).isNotBlank();
		assertThat(resp.getZoneId()).isEqualTo(1);
		assertThat(resp.getRow()).isEqualTo("A");
		assertThat(resp.getColumn()).isEqualTo("7");
		assertThat(resp.getCreatedOn()).isNotNull();
	}

	@Test
	void purchaseTicket_whenSeatAlreadyOccupied_throws_andNoEventPublished() {
		// Arrange
		SeatOccupiedRedisFacade seat = mock(SeatOccupiedRedisFacade.class);
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		TicketMapper ticketMapper = mock(TicketMapper.class);

		TicketPurchaseService svc = new TicketPurchaseService(ticketMapper, seat, eventPublisher);

		var req = new TicketPurchaseRequestDTO("V1", "E1", 1, "A", "7");

		// Mock Seat taken
		doThrow(new SeatOccupiedException("occupied"))
				.when(seat).tryOccupySeat("E1", "V1", 1, "A", "7");

		// Act & Assert
		assertThatThrownBy(() -> svc.purchaseTicket(req))
				.isInstanceOf(SeatOccupiedException.class)
				.hasMessageContaining("occupied");

		verify(eventPublisher, never()).publishEvent(any(TicketCreatedEvent.class));
	}

	@Test
	void purchaseTicket_whenMapperFails_releaseSeat_andThrowCreateTicketException() {
		// Arrange
		SeatOccupiedRedisFacade seat = mock(SeatOccupiedRedisFacade.class);
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		TicketMapper ticketMapper = mock(TicketMapper.class);

		TicketPurchaseService svc = new TicketPurchaseService(ticketMapper, seat, eventPublisher);

		var req = new TicketPurchaseRequestDTO("V1", "E1", 1, "A", "7");

		// Mock Mapper
		when(ticketMapper.toEntity(any())).thenThrow(new RuntimeException("mapper failed"));

		// Act & Assert
		assertThatThrownBy(() -> svc.purchaseTicket(req))
				.isInstanceOf(CreateTicketException.class)
				.hasMessageContaining("Failed to create ticket");

		verify(seat).releaseSeat("E1", "V1", 1, "A", "7");

		verify(eventPublisher, never()).publishEvent(any(TicketCreatedEvent.class));
	}

	@Test
	void purchaseTicket_whenEventPublisherFails_releaseSeat_andThrowCreateTicketException() {
		// Arrange
		SeatOccupiedRedisFacade seat = mock(SeatOccupiedRedisFacade.class);
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		TicketMapper ticketMapper = mock(TicketMapper.class);

		when(ticketMapper.toEntity(any())).thenReturn(new TicketInfo());
		when(ticketMapper.toRespondDto(any())).thenReturn(new TicketRespondDTO("test", 1, "A", "7", null));

		TicketPurchaseService svc = new TicketPurchaseService(ticketMapper, seat, eventPublisher);

		var req = new TicketPurchaseRequestDTO("V1", "E1", 1, "A", "7");

		// Mock EventPublisher error
		doThrow(new RuntimeException("event publisher down"))
				.when(eventPublisher).publishEvent(any(TicketCreatedEvent.class));

		// Act & Assert
		assertThatThrownBy(() -> svc.purchaseTicket(req))
				.isInstanceOf(CreateTicketException.class)
				.hasMessageContaining("Failed to create ticket");

		// Check seat released
		verify(seat).releaseSeat("E1", "V1", 1, "A", "7");
	}

	@Test
	void purchaseTicket_verifyEventContainsAllRequiredFields() {
		// Arrange
		SeatOccupiedRedisFacade seat = mock(SeatOccupiedRedisFacade.class);
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		TicketMapper ticketMapper = mock(TicketMapper.class);

		when(ticketMapper.toEntity(any())).thenReturn(new TicketInfo());
		when(ticketMapper.toRespondDto(any())).thenReturn(new TicketRespondDTO("test", 2, "B", "10", null));

		TicketPurchaseService svc = new TicketPurchaseService(ticketMapper, seat, eventPublisher);

		var req = new TicketPurchaseRequestDTO("V2", "E2", 2, "B", "10");

		// Act
		svc.purchaseTicket(req);

		ArgumentCaptor<TicketCreatedEvent> captor = ArgumentCaptor.forClass(TicketCreatedEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());

		TicketCreatedEvent event = captor.getValue();

		assertThat(event.getTicketId()).isNotBlank();
		assertThat(event.getVenueId()).isEqualTo("V2");
		assertThat(event.getEventId()).isEqualTo("E2");
		assertThat(event.getZoneId()).isEqualTo(2);
		assertThat(event.getRow()).isEqualTo("B");
		assertThat(event.getColumn()).isEqualTo("10");
		assertThat(event.getStatus()).isEqualTo(TicketStatus.PAID);
		assertThat(event.getCreatedOn()).isNotNull();

		assertThat(event.getPartitionKey()).isEqualTo("V2");
	}
}
