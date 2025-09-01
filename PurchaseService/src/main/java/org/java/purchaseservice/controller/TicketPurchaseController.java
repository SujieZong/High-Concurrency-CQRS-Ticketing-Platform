package org.java.purchaseservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.java.purchaseservice.dto.TicketPurchaseRequestDTO;
import org.java.purchaseservice.dto.TicketRespondDTO;
import org.java.purchaseservice.service.TicketPurchaseServiceInterface;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

// Received HTTP requests
@RestController
@RequestMapping("/api/v1/tickets") //Spring Controller Route
@RequiredArgsConstructor
public class TicketPurchaseController {

	private final TicketPurchaseServiceInterface ticketService;

	@PostMapping
	public ResponseEntity<TicketRespondDTO> purchaseTicket(@RequestBody @Valid TicketPurchaseRequestDTO requestDTO,
	                                                       UriComponentsBuilder uriBuilder) {
		// Use the new TicketPurchaseService
		TicketRespondDTO ticketResponse = ticketService.purchaseTicket(requestDTO);

		URI location = uriBuilder
				.path("/{id}")
				.buildAndExpand(ticketResponse.getTicketId())
				.toUri();
		return ResponseEntity.created(location).body(ticketResponse);
	}
}