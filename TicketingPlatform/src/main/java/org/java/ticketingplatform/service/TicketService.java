package org.java.ticketingplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.ticketingplatform.dto.MqDTO;
import org.java.ticketingplatform.dto.TicketCreationDTO;
import org.java.ticketingplatform.dto.TicketRespondDTO;
import org.java.ticketingplatform.exception.CreateTicketException;
import org.java.ticketingplatform.exception.SeatOccupiedException;
import org.java.ticketingplatform.mapper.TicketMapper;
import org.java.ticketingplatform.model.TicketInfo;
import org.java.ticketingplatform.model.TicketStatus;
import org.java.ticketingplatform.repository.mysql.TicketInfoRepository;
import org.java.ticketingplatform.service.rabbitmq.RabbitProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketService implements TicketServiceInterface {

	private final TicketMapper ticketMapper;
	private final SeatOccupiedRedisFacade seatOccupiedRedisFacade;
	private final RabbitProducer rabbitProducer;
	private final TicketInfoRepository ticketInfoRepository;

	// transfer input data into a Response DTO object and save to Database through DAO and Mapper
	@Override
	@Transactional
	public TicketRespondDTO purchaseTicket(TicketCreationDTO dto) {
		log.debug("[TicketService][purchaseTicket] start with creation request: {}", dto);

		//Redis - Set Redis seat occupancy to a True - Lua script
		try {
			seatOccupiedRedisFacade.tryOccupySeat(dto.getEventId(), dto.getVenueId(), dto.getZoneId(), dto.getRow(), dto.getColumn());
			log.debug("[TicketService][purchaseTicket] Redis SeatOccupy Success for event={}", dto.getEventId());
		} catch (SeatOccupiedException e) {
			log.warn("[TicketService][purchaseTicket] !Redis SeatOccupy FAILED!, event={}", dto.getEventId());
			throw new SeatOccupiedException(e.getMessage());
		}

		TicketInfo ticket = ticketMapper.toEntity(dto);
		ticket.setTicketId(generateTicketId(dto));   // 不信输入，无则生成
		ticket.setCreatedOn(Instant.now());
		ticket.setStatus(TicketStatus.PENDING_PAYMENT);


		try {
			TicketInfo saved = ticketInfoRepository.save(ticket);
			log.info("[TicketService][purchaseTicket] created ticketId={}", saved.getTicketId());

			TicketStatus currentStatus = saved.getStatus();

			MqDTO mqMessage = new MqDTO(
					saved.getTicketId(), saved.getVenueId(),
					saved.getEventId(), saved.getZoneId(),
					saved.getRow(), saved.getColumn(),
					currentStatus.name(), saved.getCreatedOn());
			rabbitProducer.sendTicketCreated(mqMessage);
			log.info("[TicketService][purchaseTicket] Sent ticket.created event for ticketId={}", saved.getTicketId());


			return ticketMapper.toRespondDto(saved);      // 响应基于“保存后的真数据”
		} catch (Exception ex) {
			safeReleaseSeat(dto, ticket.getTicketId(), ex); // 失败补偿
			throw new CreateTicketException("Failed to create ticket", ex);
		}
	}

	// 步骤 4: (可选，但推荐) 在Redis中为座位设置一个过期时间，用于自动释放
	// seatOccupiedRedisFacade.setHoldExpiration(ticketCreation.getEventId(), ...);

	private String generateTicketId(TicketCreationDTO dto) {
		// 若前端传了 id 且格式合法，可保留；否则生成
		String input = dto.getId();
		if (input != null && !input.isBlank()) return input;
		return UUID.randomUUID().toString();
		// 若你要“可读ID”，保留你的 TKT-%s-%s-%06d 方案，但要防碰撞
	}

	private void safeReleaseSeat(TicketCreationDTO dto, String ticketId, Exception original) {
		try {
			seatOccupiedRedisFacade.releaseSeat(dto.getEventId(), dto.getVenueId(), dto.getZoneId(), dto.getRow(), dto.getColumn());
			log.info("[TicketService][purchaseTicket] released seat after DB failure, ticketId={}", ticketId);
		} catch (Exception re) {
			log.error("[TicketService][purchaseTicket] RELEASE FAILED ticketId={}, cause={}, releaseErr={}", ticketId, original.getMessage(), re.getMessage(), re);
			// 这里可以打告警/落 Outbox 死信表，留待人工处理
		}
	}
}
