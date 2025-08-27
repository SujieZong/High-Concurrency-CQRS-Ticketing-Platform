package org.java.ticketingplatform.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.ticketingplatform.dto.TicketInfoDTO;
import org.java.ticketingplatform.exception.TicketNotFoundException;
import org.java.ticketingplatform.mapper.TicketMapper;
import org.java.ticketingplatform.model.TicketInfo;
import org.java.ticketingplatform.repository.DynamoTicketDAOInterface;
import org.java.ticketingplatform.repository.mysql.TicketInfoRepository;
import org.java.ticketingplatform.service.QueryServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService implements QueryServiceInterface {
	private final TicketInfoRepository ticketInfoRepository;
	private final TicketMapper tickerMapper;
	private final DynamoTicketDAOInterface dynamoTicketDao;

	@Override
	@Transactional(readOnly = true)
	public TicketInfoDTO getTicket(String ticketId) {
		log.debug("[TicketService][getTicket] start query ticketId={}", ticketId);

		TicketInfo ticketEntity = dynamoTicketDao.getTicketInfoById(ticketId);

		if (ticketEntity == null) {
			throw new TicketNotFoundException("Ticket not found with ID: " + ticketId);
		}

		return tickerMapper.toInfoDto(ticketEntity);
	}

	@Override
	@Transactional(readOnly = true)
	public int countTicketSoldByEvent(String eventId) {
		log.debug("[QueryService][countTicketSoldByEvent] start for eventId={}", eventId);
		int count = ticketInfoRepository.countByEventId(eventId);
		log.debug("[QueryService][countTicketSoldByEvent] result={} for eventId={}", count, eventId);
		return count;
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal sumRevenueByVenueAndEvent(String venueId, String eventId) {
		log.debug("[QueryService][sumRevenueByVenueAndEvent] start venueId={},eventId={}", venueId, eventId);
		BigDecimal revenue = ticketInfoRepository.sumRevenueByVenueAndEvent(venueId, eventId);
		log.debug("[QueryService][sumRevenueByVenueAndEvent] result={} for venueId={},eventId={}",
				revenue, venueId, eventId);
		return revenue == null ? BigDecimal.ZERO : revenue;
	}
}
