package org.java.ticketingplatform.controller;

import org.java.ticketingplatform.dto.TicketCreationDTO;
import org.java.ticketingplatform.dto.TicketPurchaseRequestDTO;
import org.java.ticketingplatform.dto.TicketRespondDTO;
import org.java.ticketingplatform.service.TicketServiceInterface;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketPurchaseController {

	private final TicketServiceInterface ticketService;

	public TicketPurchaseController(TicketServiceInterface ticketService) {
		this.ticketService = ticketService;
	}

	@PostMapping("/tickets")
	public ResponseEntity<TicketRespondDTO> purchaseTicket(@RequestBody TicketPurchaseRequestDTO requestDTO) {

		String ticketId = UUID.randomUUID().toString();
		Instant creationTime = Instant.now();

		TicketCreationDTO creationDTO = new TicketCreationDTO(
				ticketId,
				requestDTO.getVenueId(),
				requestDTO.getEventId(),
				requestDTO.getZoneId(),
				requestDTO.getRow(),
				requestDTO.getColumn(),
				creationTime
		);

		// Use the new TicketService
		TicketRespondDTO ticketResponse = ticketService.purchaseTicket(creationDTO);

		return ResponseEntity.status(HttpStatus.CREATED).body(ticketResponse);
	}

}