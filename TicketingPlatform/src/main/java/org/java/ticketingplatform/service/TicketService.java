package org.java.ticketingplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.ticketingplatform.dto.MqDTO;
import org.java.ticketingplatform.dto.TicketCreationDTO;
import org.java.ticketingplatform.dto.TicketInfoDTO;
import org.java.ticketingplatform.dto.TicketRespondDTO;
import org.java.ticketingplatform.exception.SeatOccupiedException;
import org.java.ticketingplatform.model.TicketInfo;
import org.java.ticketingplatform.model.TicketStatus;
import org.java.ticketingplatform.repository.mysql.TicketInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketService implements TicketServiceInterface {

	//	private final TicketMapper ticketMapper;
	private final SeatOccupiedRedisFacade seatOccupiedRedisFacade;
	//	private final RabbitProducer rabbitProducer;
	private final TicketInfoRepository ticketInfoRepository;

	// transfer input data into a Response DTO object and save to Database through DAO and Mapper
	@Override
	@Transactional
	public TicketRespondDTO purchaseTicket(TicketCreationDTO DTO) {
		log.debug("[TicketService][purchaseTicket] start with creation request: {}", DTO);

		//Redis - Set Redis seat occupancy to a True - Lua script
		try {
			seatOccupiedRedisFacade.tryOccupySeat(
					DTO.getEventId(),
					DTO.getVenueId(),
					DTO.getZoneId(),
					DTO.getRow(),
					DTO.getColumn());
			log.debug("[TicketService][purchaseTicket] Redis SeatOccupy Success for event={}", DTO.getEventId());
		} catch (SeatOccupiedException e) {
			log.warn("[TicketService][purchaseTicket] !Redis SeatOccupy FAILED!, event={}", DTO.getEventId());
			throw new SeatOccupiedException(e.getMessage());
		}

		//Ticket DTO, Database Write Model through mapper
		Instant timeNow = Instant.now();
		TicketInfo newOrder = new TicketInfo();
		newOrder.setTicketId(DTO.getId());
		newOrder.setVenueId(DTO.getVenueId());
		newOrder.setEventId(DTO.getEventId());
		newOrder.setZoneId(DTO.getZoneId());
		newOrder.setRow(DTO.getRow());
		newOrder.setColumn(DTO.getColumn());
		newOrder.setCreatedOn(DTO.getCreatedOn());
		newOrder.setStatus(TicketStatus.PENDING_PAYMENT);

		try {
			ticketInfoRepository.save(newOrder);
			log.info("[TicketService][purchaseTicket] Successfully created PENDING_PAYMENT order with id={}",
					newOrder.getTicketId());
		} catch (Exception e) {
			// Release in Redis if failed
			seatOccupiedRedisFacade.releaseSeat(
					DTO.getEventId(),
					DTO.getVenueId(),
					DTO.getZoneId(),
					DTO.getRow(),
					DTO.getColumn());
			log.error("[TicketService][purchaseTicket] Failed to save order to DB, released Redis seat. OrderId={}",
					newOrder.getTicketId(), e);
			throw new RuntimeException("Failed to create order, please try again.");
		}

		// 步骤 4: (可选，但推荐) 在Redis中为座位设置一个过期时间，用于自动释放
		// seatOccupiedRedisFacade.setHoldExpiration(ticketCreation.getEventId(), ...);

		TicketRespondDTO response = new TicketRespondDTO(
				newOrder.getTicketId(),
				newOrder.getZoneId(),
				newOrder.getRow(),
				newOrder.getColumn(),
				newOrder.getCreatedOn()
		);
		log.debug("[TicketService][createTicket] CreateTicket Finished, response{}", response);
		return response;
	}
}
