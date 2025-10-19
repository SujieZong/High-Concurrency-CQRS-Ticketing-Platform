package org.java.queryservice.controller;

import org.java.queryservice.dto.ErrorMessage;
import org.java.queryservice.dto.TicketInfoDTO;
import org.java.queryservice.exception.TicketNotFoundException;
import org.java.queryservice.service.QueryServiceInterface;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketQueryController {

	private final QueryServiceInterface queryService;

	public TicketQueryController(QueryServiceInterface queryService) {
		this.queryService = queryService;
	}

	@GetMapping("/{ticketId}")
	public ResponseEntity<?> getTicket(@PathVariable("ticketId") String ticketId) {
		try {
			TicketInfoDTO ticketInfoDTO = queryService.getTicket(ticketId);
			return ResponseEntity.ok(ticketInfoDTO);
		} catch (TicketNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ErrorMessage("TicketID not found: " + ticketId));
		}
	}

	@GetMapping("/tickets")
	public ResponseEntity<List<TicketInfoDTO>> getAllSoldTickets() {
		List<TicketInfoDTO> tickets = queryService.getAllSoldTickets();
		return ResponseEntity.ok(tickets);
	}

	@GetMapping("/count/{eventId}")
	public ResponseEntity<String> countSoldByEvent(@PathVariable("eventId") String eventId) {
		int count = queryService.countTicketSoldByEvent(eventId);
		String message = String.format("Tickets sold for event %s is %d", eventId, count);

		return ResponseEntity.status(HttpStatus.OK).body(message);
	}

	@GetMapping("/revenue/{venueId}/{eventId}")
	public ResponseEntity<String> revenueByEvent(
			@PathVariable("venueId") String venueId,
			@PathVariable("eventId") String eventId) {
		BigDecimal revenue = queryService.sumRevenueByVenueAndEvent(venueId, eventId);
		String message = String.format("Tickets sold for event %s in venue %s generated revenue %s", eventId, venueId,
				revenue);

		return ResponseEntity.status(HttpStatus.OK).body(message);
	}

	/**
	 * Handle root path requests with helpful information
	 */
	@GetMapping("")
	public ResponseEntity<Map<String, Object>> getRootInfo() {
		Map<String, Object> response = new HashMap<>();
		response.put("service", "QueryService");
		response.put("version", "1.0.0");
		response.put("status", "running");
		response.put("availableEndpoints", new String[] {
				"GET /api/v1/health",
				"GET /api/v1/tickets/{ticketId}",
				"GET /api/v1/tickets/tickets",
				"GET /api/v1/tickets/count/{eventId}",
				"GET /api/v1/tickets/revenue/{venueId}/{eventId}"
		});
		response.put("message", "QueryService is running. Use specific endpoints for ticket operations.");

		return ResponseEntity.ok(response);
	}
}