package org.java.ticketingplatform.service;

import lombok.extern.slf4j.Slf4j;
import org.java.ticketingplatform.dto.MqDTO;
import org.java.ticketingplatform.dto.TicketInfoDTO;
import org.java.ticketingplatform.dto.TicketRespondDTO;
import org.java.ticketingplatform.exception.SeatOccupiedException;
import org.java.ticketingplatform.mapper.TicketMapper;
import org.java.ticketingplatform.model.TicketCreation;
import org.java.ticketingplatform.model.TicketInfo;
import org.java.ticketingplatform.repository.DynamoTicketDAOInterface;
import org.java.ticketingplatform.repository.mysql.TicketInfoRepository;
import org.java.ticketingplatform.service.rabbitmq.RabbitProducer;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class TicketService implements TicketServiceInterface {

	private final TicketMapper ticketMapper;
	private final DynamoTicketDAOInterface ticketDAO;
	private final SeatOccupiedRedisFacade seatOccupiedRedisFacade;
	private final RabbitProducer rabbitProducer;
	private final TicketInfoRepository ticketInfoRepository;

	public TicketService(TicketMapper ticketMapper,
	                     DynamoTicketDAOInterface ticketDAO,
	                     SeatOccupiedRedisFacade seatOccupiedRedisFacade,
	                     RabbitProducer rabbitProducer,
	                     TicketInfoRepository ticketInfoRepository) {
		this.ticketMapper = ticketMapper;
		this.ticketDAO = ticketDAO;
		this.seatOccupiedRedisFacade = seatOccupiedRedisFacade;
		this.rabbitProducer = rabbitProducer;
		this.ticketInfoRepository = ticketInfoRepository;
	}


	// transfer input data into a Response DTO object and save to Database through DAO and Mapper
	@Override
	public TicketRespondDTO createTicket(String venueId, String eventId, int zoneId, String row, String column) {
		log.debug("[TicketService][createTicket] start with venue={}, event={}, zone={}, row={}, col={}",
				venueId, eventId, zoneId, row, column);

		//Redis - Set Redis seat occupancy to a True - Lua script
		try {
			seatOccupiedRedisFacade.tryOccupySeat(eventId, venueId, zoneId, row, column);
			log.debug("[TicketService][createTicket] Redis SeatOccupy Success for event={},venue={},zone={},row={},col={}",
					eventId, venueId, zoneId, row, column);
		} catch (SeatOccupiedException e) {
			log.warn("[TicketService][createTicket] !Redis SeatOccupy FAILED!, event={}, zone={}, row={}, col={}", eventId, zoneId, row, column);
			throw new SeatOccupiedException(e.getMessage());
		}

		//Ticket DTO, Database Write Model through mapper
		Instant timeNow = Instant.now();
		TicketInfoDTO ticketInfoDTO = new TicketInfoDTO(venueId, eventId, zoneId, row, column, timeNow);
		//turn the information to Mapper to create a DTO object
		TicketCreation ticketCreation = ticketMapper.ticketInfoDTOToTicketCreation(ticketInfoDTO);
		log.debug("[TicketService][createTicket] mapped to TicketCreation={}", ticketCreation);

		// construct a MqDTO Object and send to MQ
		// RabbitMq producer and consumer usage
		MqDTO mqMessage = new MqDTO(ticketCreation.getId(), ticketCreation.getVenueId(), ticketCreation.getEventId(), ticketCreation.getZoneId(), ticketCreation.getRow(), ticketCreation.getColumn(), ticketCreation.getStatus(), ticketCreation.getCreatedOn());
		try {
			rabbitProducer.sendTicketCreated(mqMessage);
			log.debug("[TicketService][createTicket] RabbitMQ Message sent to queue success, ticketId = {}", ticketCreation.getId());
		} catch (Exception e) {
			seatOccupiedRedisFacade.releaseSeat(eventId, venueId, zoneId, row, column);
			log.error("[TicketService][createTicket] RabbitMQ send failed, released seat, ticketId={}, cause={}", ticketCreation.getId(), e.getMessage());
			throw e;
		}

		TicketRespondDTO response = ticketMapper.ticketCreationToTicketRespondDTO(ticketCreation);
		log.debug("[TicketService][createTicket] CreateTicket Finished, response{}", response);
		return response;
	}

	// call get from DAO
	@Override
	public TicketInfo getTicket(String ticketId) {
		log.debug("[TicketService][getTicket] start query ticketId={}", ticketId);
		TicketInfo info = ticketDAO.getTicketInfoById(ticketId);
		if (info == null) {
			log.trace("[TicketService][getTicket] NOT FOUND! ticketId={}", ticketId);
		} else {
			log.debug("[TicketService][getTicket] Found, ticket={}", info);
		}
		return info;
	}
}
