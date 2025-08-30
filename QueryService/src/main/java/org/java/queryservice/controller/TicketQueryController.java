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

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketQueryController {

	private final QueryServiceInterface queryService;

	public TicketQueryController(QueryServiceInterface queryService) {
		this.queryService = queryService;
	}


	@GetMapping("/{ticketId}")
	public ResponseEntity<?> getTicket(@PathVariable String ticketId) {
		try {
			TicketInfoDTO ticketInfoDTO = queryService.getTicket(ticketId);
			return ResponseEntity.ok(ticketInfoDTO);
		} catch (TicketNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ErrorMessage("TicketID not found: " + ticketId));
		}
	}


	@GetMapping("/count/{eventId}")
	public ResponseEntity<String> countSoldByEvent(@PathVariable String eventId) {
		int count = queryService.countTicketSoldByEvent(eventId);
		String message = String.format("Tickets sold for event %s is %d", eventId, count);

		return ResponseEntity.status(HttpStatus.OK).body(message);
	}

	@GetMapping("/money/{venueId}/{eventId}")
	public ResponseEntity<String> moneyByEvent(
			@PathVariable String venueId, @PathVariable String eventId) {
		BigDecimal revenue = queryService.sumRevenueByVenueAndEvent(venueId, eventId);
		String message = String.format(
				"Tickets sold for event %s in venue %s generated revenue %s",
				eventId, venueId, revenue);

		return ResponseEntity.status(HttpStatus.OK).body(message);
	}
}