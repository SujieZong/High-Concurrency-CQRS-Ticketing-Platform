package org.java.queryservice.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.queryservice.dto.TicketInfoDTO;
import org.java.queryservice.exception.TicketNotFoundException;
import org.java.queryservice.mapper.TicketMapper;
import org.java.queryservice.model.TicketInfo;
import org.java.queryservice.repository.mysql.TicketInfoRepository;
import org.java.queryservice.service.QueryServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService implements QueryServiceInterface {
	private final TicketInfoRepository ticketInfoRepository;
	private final TicketMapper tickerMapper;

	// find ticket by ID
	@Override
	@Transactional(readOnly = true)
	public TicketInfoDTO getTicket(String ticketId) {
		log.debug("[QueryService][getTicket] start query ticketId={}", ticketId);

		var ticketEntity = ticketInfoRepository.findById(ticketId)
				.orElseThrow(() -> new TicketNotFoundException("Ticket not found with ID: " + ticketId));

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

	@Override
	@Transactional(readOnly = true)
	public List<TicketInfoDTO> getAllSoldTickets() {
		log.debug("[QueryService][getAllSoldTickets] start");
		List<TicketInfo> tickets = ticketInfoRepository.findAll();
		List<TicketInfoDTO> result = tickets.stream()
				.map(tickerMapper::toInfoDto)
				.collect(Collectors.toList());
		log.debug("[QueryService][getAllSoldTickets] found {} tickets", result.size());
		return result;
	}
}
