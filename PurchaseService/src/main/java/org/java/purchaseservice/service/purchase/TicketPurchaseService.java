package org.java.purchaseservice.service.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.purchaseservice.dto.TicketCreationDTO;
import org.java.purchaseservice.dto.TicketPurchaseRequestDTO;
import org.java.purchaseservice.dto.TicketRespondDTO;
import org.java.purchaseservice.event.TicketCreatedEvent;
import org.java.purchaseservice.exception.CreateTicketException;
import org.java.purchaseservice.exception.SeatOccupiedException;
import org.java.purchaseservice.mapper.TicketMapper;
import org.java.purchaseservice.model.TicketInfo;
import org.java.purchaseservice.model.TicketStatus;
import org.java.purchaseservice.service.TicketPurchaseServiceInterface;
import org.java.purchaseservice.service.redis.SeatOccupiedRedisFacade;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

// Service: Generate ticketId/timestamp → Redis seat lock → Publish event to Kafka (event-sourced CQRS)
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketPurchaseService implements TicketPurchaseServiceInterface {

	private final TicketMapper ticketMapper;
	private final SeatOccupiedRedisFacade seatOccupiedRedisFacade;
	private final ApplicationEventPublisher eventPublisher;

	// Persistent through Kafka by spring event
	@Override
	@Transactional
	public TicketRespondDTO purchaseTicket(TicketPurchaseRequestDTO dto) {
		log.info("[TicketPurchaseService] purchaseTicket start: eventId={}, zone={}, row={}, col={}", dto.getEventId(),
				dto.getZoneId(), dto.getRow(), dto.getColumn());

		// Part 1: Redis - Set Redis seat occupancy to a True - Lua script
		try {
			seatOccupiedRedisFacade.tryOccupySeat(dto.getEventId(), dto.getVenueId(), dto.getZoneId(), dto.getRow(),
					dto.getColumn());
			log.debug("[TicketPurchaseService] seat occupied OK for eventId={}, seat={}-{}", dto.getEventId(),
					dto.getRow(), dto.getColumn());
		} catch (SeatOccupiedException e) {
			log.warn("[TicketPurchaseService] seat already occupied: eventId={}, seat={}-{}", dto.getEventId(),
					dto.getRow(), dto.getColumn());
			throw e;
		}

		// -- Part 2 Generation UUID and time--
		String ticketId = UUID.randomUUID().toString();
		Instant now = Instant.now();

		try {
			// -- Part 3: Construct ticket data model (not persisted in write service)
			TicketCreationDTO creation = TicketCreationDTO.builder().ticketId(ticketId).venueId(dto.getVenueId())
					.eventId(dto.getEventId()).zoneId(dto.getZoneId()).row(dto.getRow()).column(dto.getColumn())
					.status(TicketStatus.PAID).createdOn(now).build();

			// Build entity for event publishing (CQRS: event is the source of truth)
			TicketInfo entity = ticketMapper.toEntity(creation);
			entity.setTicketId(ticketId);
			entity.setStatus(creation.getStatus());
			entity.setCreatedOn(now);

			// -- Part 4: Publish Spring Event to Kafka (event-sourced architecture)
			TicketCreatedEvent event = TicketCreatedEvent.builder().ticketId(ticketId).venueId(dto.getVenueId())
					.eventId(dto.getEventId()).zoneId(dto.getZoneId()).row(dto.getRow()).column(dto.getColumn())
					.status(creation.getStatus()).createdOn(now).build();

			eventPublisher.publishEvent(event);
			log.info("[TicketPurchaseService] TicketCreatedEvent published: ticketId={}", ticketId);

			// direct return
			return ticketMapper.toRespondDto(entity);

		} catch (Exception ex) {
			// any error, release seat
			safeReleaseSeat(dto, ticketId, ex);
			throw new CreateTicketException("Failed to create ticket", ex);
		}
	}

	// Release seat from Redis
	private void safeReleaseSeat(TicketPurchaseRequestDTO dto, String ticketId, Exception original) {
		try {
			seatOccupiedRedisFacade.releaseSeat(dto.getEventId(), dto.getVenueId(), dto.getZoneId(), dto.getRow(),
					dto.getColumn());
			log.info("[TicketPurchaseService] seat released after failure, ticketId={}", ticketId);
		} catch (Exception re) {
			log.error("[TicketPurchaseService] seat release FAILED, ticketId={}, cause={}, releaseErr={}", ticketId,
					original.getMessage(), re.getMessage(), re);
		}
	}
}