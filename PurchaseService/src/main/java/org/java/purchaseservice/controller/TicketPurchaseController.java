package org.java.purchaseservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.java.purchaseservice.dto.TicketPurchaseRequestDTO;
import org.java.purchaseservice.dto.TicketRespondDTO;
import org.java.purchaseservice.service.TicketPurchaseServiceInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.HashMap;

// Received HTTP requests
@RestController
@RequestMapping("/api/v1/tickets") // Spring Controller Route
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

	/**
	 * Handle unsupported HTTP methods with helpful message
	 */
	@RequestMapping(method = { RequestMethod.GET, RequestMethod.PUT, RequestMethod.DELETE })
	public ResponseEntity<Map<String, Object>> handleUnsupportedMethod() {
		Map<String, Object> response = new HashMap<>();
		response.put("error", "Method Not Allowed");
		response.put("message", "PurchaseService only supports POST requests for ticket purchasing");
		response.put("supportedMethod", "POST");
		response.put("endpoint", "/api/v1/tickets");
		response.put("description", "Use POST to purchase tickets");

		return ResponseEntity.status(405).body(response);
	}
}