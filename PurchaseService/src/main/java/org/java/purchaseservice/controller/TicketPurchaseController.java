package org.java.purchaseservice.controller;

import org.java.purchaseservice.dto.TicketCreationDTO;
import org.java.purchaseservice.dto.TicketPurchaseRequestDTO;
import org.java.purchaseservice.dto.TicketRespondDTO;
import org.java.purchaseservice.service.TicketPurchaseServiceInterface;
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

	private final TicketPurchaseServiceInterface ticketService;

	public TicketPurchaseController(TicketPurchaseServiceInterface ticketService) {
		this.ticketService = ticketService;
	}

	@PostMapping
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
				"PAID",
				creationTime
		);

		// Use the new TicketPurchaseService
		TicketRespondDTO ticketResponse = ticketService.purchaseTicket(creationDTO);

		return ResponseEntity.status(HttpStatus.CREATED).body(ticketResponse);
	}

}