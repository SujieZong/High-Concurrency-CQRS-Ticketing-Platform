package org.java.purchaseservice.service.purchase;

import java.time.Instant;
import java.util.UUID;

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

/**
 * Service for ticket purchase: Redis seat lock → Generate ticket → Publish
 * event to Kafka.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketPurchaseService implements TicketPurchaseServiceInterface {

	private final TicketMapper ticketMapper;
	private final SeatOccupiedRedisFacade seatOccupiedRedisFacade;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	public TicketRespondDTO purchaseTicket(final TicketPurchaseRequestDTO dto) {
		log.info(
				"[TicketPurchaseService] Starting purchase: eventId={}, seat={}-{}-{}",
				dto.getEventId(),
				dto.getZoneId(),
				dto.getRow(),
				dto.getColumn());

		// Reserve seat in Redis
		validateAndOccupySeat(dto);

		final String ticketId = UUID.randomUUID().toString();
		final Instant now = Instant.now();

		try {
			// Create ticket data
			final TicketCreationDTO creation = buildTicketCreationData(dto, ticketId, now);

			// Build entity for response (CQRS: write model)
			final TicketInfo entity = buildTicketEntity(creation);

			// Publish event to Kafka (CQRS: event sourcing)
			publishTicketCreatedEvent(creation);

			// Return response (CQRS: read model will be updated asynchronously)
			return ticketMapper.toRespondDto(entity);

		} catch (Exception ex) {
			safeReleaseSeat(dto, ticketId, ex);
			throw new CreateTicketException("Failed to create ticket", ex);
		}
	}

	private void validateAndOccupySeat(final TicketPurchaseRequestDTO dto) {
		try {
			seatOccupiedRedisFacade.tryOccupySeat(
					dto.getEventId(), dto.getVenueId(), dto.getZoneId(), dto.getRow(), dto.getColumn());
			log.debug(
					"[TicketPurchaseService] Seat occupied: eventId={}, seat={}-{}",
					dto.getEventId(),
					dto.getRow(),
					dto.getColumn());
		} catch (SeatOccupiedException e) {
			log.warn(
					"[TicketPurchaseService] Seat already occupied: eventId={}, seat={}-{}",
					dto.getEventId(),
					dto.getRow(),
					dto.getColumn());
			throw e;
		}
	}

	private TicketCreationDTO buildTicketCreationData(
			final TicketPurchaseRequestDTO dto, final String ticketId, final Instant now) {
		return TicketCreationDTO.builder()
				.ticketId(ticketId)
				.venueId(dto.getVenueId())
				.eventId(dto.getEventId())
				.zoneId(dto.getZoneId())
				.row(dto.getRow())
				.column(dto.getColumn())
				.status(TicketStatus.PAID)
				.createdOn(now)
				.build();
	}

	private TicketInfo buildTicketEntity(final TicketCreationDTO creation) {
		final TicketInfo entity = ticketMapper.toEntity(creation);
		entity.setTicketId(creation.getTicketId());
		entity.setStatus(creation.getStatus());
		entity.setCreatedOn(creation.getCreatedOn());
		return entity;
	}

	private void publishTicketCreatedEvent(final TicketCreationDTO creation) {
		final TicketCreatedEvent event = TicketCreatedEvent.builder()
				.ticketId(creation.getTicketId())
				.venueId(creation.getVenueId())
				.eventId(creation.getEventId())
				.zoneId(creation.getZoneId())
				.row(creation.getRow())
				.column(creation.getColumn())
				.status(creation.getStatus())
				.createdOn(creation.getCreatedOn())
				.build();

		eventPublisher.publishEvent(event);
		log.info("[TicketPurchaseService] Event published: ticketId={}", creation.getTicketId());
	}

	private void safeReleaseSeat(
			final TicketPurchaseRequestDTO dto, final String ticketId, final Exception original) {
		try {
			seatOccupiedRedisFacade.releaseSeat(
					dto.getEventId(), dto.getVenueId(), dto.getZoneId(), dto.getRow(), dto.getColumn());
			log.info("[TicketPurchaseService] Seat released after failure: ticketId={}", ticketId);
		} catch (Exception re) {
			log.error(
					"[TicketPurchaseService] Seat release failed: ticketId={}, original={}, releaseError={}",
					ticketId,
					original.getMessage(),
					re.getMessage(),
					re);
		}
	}
}
