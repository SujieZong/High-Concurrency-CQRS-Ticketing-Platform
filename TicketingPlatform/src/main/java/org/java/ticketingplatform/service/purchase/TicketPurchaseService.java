package org.java.ticketingplatform.service.purchase;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.ticketingplatform.dto.TicketCreationDTO;
import org.java.ticketingplatform.dto.TicketRespondDTO;
import org.java.ticketingplatform.exception.CreateTicketException;
import org.java.ticketingplatform.exception.SeatOccupiedException;
import org.java.ticketingplatform.service.redis.SeatOccupiedRedisFacade;
import org.java.ticketingplatform.service.TicketPurchaseServiceInterface;
import org.java.ticketingplatform.service.outbox.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketPurchaseService implements TicketPurchaseServiceInterface {

	//	private final TicketMapper ticketMapper;
	private final SeatOccupiedRedisFacade seatOccupiedRedisFacade;
	private final OutboxService outboxService;
	private final ObjectMapper objectMapper;

	// transfer input data into a Response DTO object and save to Database through DAO and Mapper
	@Override
	@Transactional
	public TicketRespondDTO purchaseTicket(TicketCreationDTO dto) {
		log.info("[TicketPurchaseService] purchaseTicket start: eventId={}, zone={}, row={}, col={}",
				dto.getEventId(), dto.getZoneId(), dto.getRow(), dto.getColumn());


		//Redis - Set Redis seat occupancy to a True - Lua script
		try {
			seatOccupiedRedisFacade.tryOccupySeat(
					dto.getEventId(),
					dto.getVenueId(),
					dto.getZoneId(),
					dto.getRow(),
					dto.getColumn()
			);
			log.debug("[TicketPurchaseService] seat occupied OK for eventId={}, seat={}-{}",
					dto.getEventId(), dto.getRow(), dto.getColumn());
		} catch (SeatOccupiedException e) {
			log.warn("[TicketPurchaseService] seat already occupied: eventId={}, seat={}-{}",
					dto.getEventId(), dto.getRow(), dto.getColumn());
			throw e;
		}

		String ticketId = UUID.randomUUID().toString();
		Instant now = Instant.now();

		try {
			Map<String, Object> event = new HashMap<>();
			event.put("ticketId", ticketId);
			event.put("venueId", dto.getVenueId());
			event.put("eventId", dto.getEventId());
			event.put("zoneId", dto.getZoneId());
			event.put("row", dto.getRow());
			event.put("column", dto.getColumn());
			event.put("status", "PAID");
			event.put("createdOn", now.toString());

			String payload = objectMapper.writeValueAsString(event);

			outboxService.saveEvent("ticket.created", payload);

			// direct return
			return new TicketRespondDTO(
					ticketId,
					dto.getZoneId(),
					dto.getRow(),
					dto.getColumn(),
					now
			);

		} catch (Exception ex) {
			// any error , release seat
			safeReleaseSeat(dto, ticketId, ex);
			throw new CreateTicketException("Failed to create ticket", ex);
		}
	}

	private void safeReleaseSeat(TicketCreationDTO dto, String ticketId, Exception original) {
		try {
			seatOccupiedRedisFacade.releaseSeat(
					dto.getEventId(),
					dto.getVenueId(),
					dto.getZoneId(),
					dto.getRow(),
					dto.getColumn()
			);
			log.info("[TicketPurchaseService] seat released after failure, ticketId={}", ticketId);
		} catch (Exception re) {
			log.error("[TicketPurchaseService] seat release FAILED, ticketId={}, cause={}, releaseErr={}",
					ticketId, original.getMessage(), re.getMessage(), re);
		}
	}
}