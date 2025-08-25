package org.java.ticketingplatform.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.ticketingplatform.dto.TicketInfoDTO;
import org.java.ticketingplatform.exception.TicketNotFoundException;
import org.java.ticketingplatform.model.TicketInfo;
import org.springframework.transaction.annotation.Transactional;
import org.java.ticketingplatform.repository.mysql.TicketInfoRepository;
import org.java.ticketingplatform.service.QueryServiceInterface;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService implements QueryServiceInterface {
	private final TicketInfoRepository ticketInfoRepository;


	@Override
	@Transactional(readOnly = true)
	public TicketInfo getTicket(String ticketId) {
		log.debug("[TicketService][getTicket] start query ticketId={}", ticketId);

		return ticketInfoRepository.findById(ticketId)
				.orElseThrow(() -> {
					log.warn("[QueryService][getTicket] NOT FOUND! ticketId={}", ticketId);
					return new TicketNotFoundException("Ticket not found with id: " + ticketId);
				});
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
