package org.java.purchaseservice.service.purchase;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.purchaseservice.dto.TicketPurchaseRequestDTO;
import org.java.purchaseservice.exception.CreateTicketException;
import org.java.purchaseservice.exception.SeatOccupiedException;
import org.java.purchaseservice.mapper.TicketMapper;
import org.java.purchaseservice.model.TicketInfo;
import org.java.purchaseservice.model.TicketStatus;
import org.java.purchaseservice.repository.DynamoTicketDaoInterface;
import org.java.purchaseservice.service.TicketPurchaseServiceInterface;
import org.java.purchaseservice.service.outbox.OutboxService;
import org.java.purchaseservice.service.redis.SeatOccupiedRedisFacade;
import org.java.purchaseservice.dto.TicketCreationDTO;
import org.java.purchaseservice.dto.TicketRespondDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//Service：统一生成 ticketId/createdOn，编排占座 → 写库 → 记 Outbox → 返回
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketPurchaseService implements TicketPurchaseServiceInterface {

	private final TicketMapper ticketMapper;
	private final SeatOccupiedRedisFacade seatOccupiedRedisFacade;
	private final OutboxService outboxService;
	private final ObjectMapper objectMapper;
	private final DynamoTicketDaoInterface dynamoTicketDao;

	// transfer input data into a Response DTO object and save to Database through DAO and Mapper
	@Override
	@Transactional
	public TicketRespondDTO purchaseTicket(TicketPurchaseRequestDTO dto) {
		log.info("[TicketPurchaseService] purchaseTicket start: eventId={}, zone={}, row={}, col={}",
				dto.getEventId(), dto.getZoneId(), dto.getRow(), dto.getColumn());

		//Part 1: Redis - Set Redis seat occupancy to a True - Lua script
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

		// -- Part 2 Generation UUID and time--
		String ticketId = UUID.randomUUID().toString();
		Instant now = Instant.now();

		try {
			// -- part 3 Write to DynamoDB -> construct DTO then write
			TicketCreationDTO creation = TicketCreationDTO.builder()
					.ticketId(ticketId)
					.venueId(dto.getVenueId())
					.eventId(dto.getEventId())
					.zoneId(dto.getZoneId())
					.row(dto.getRow())
					.column(dto.getColumn())
					.status(TicketStatus.PAID)
					.createdOn(now)
					.build();

			//
			TicketInfo entity = ticketMapper.toEntity(creation);
			entity.setTicketId(ticketId);
			entity.setStatus(creation.getStatus());
			entity.setCreatedOn(now);

			// -- Part 4 -- write into DynamoDB, Direct Write
			dynamoTicketDao.createTicket(entity);

			//-- Part 5 --  for Outbox event recording
			Map<String, Object> event = new HashMap<>();
			event.put("ticketId", ticketId);
			event.put("venueId", dto.getVenueId());
			event.put("eventId", dto.getEventId());
			event.put("zoneId", dto.getZoneId());
			event.put("row", dto.getRow());
			event.put("column", dto.getColumn());
			event.put("status", creation.getStatus());
			event.put("createdOn", now.toString());

			//serialize and send to outbox
			String payload = objectMapper.writeValueAsString(event);
			outboxService.saveEvent("ticket.created", payload);

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