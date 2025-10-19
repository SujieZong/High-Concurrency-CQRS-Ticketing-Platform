package org.java.queryservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TicketQueryHealthController {
	@GetMapping("/health")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok("Query Service is healthy! Available endpoints: /tickets/{id}, /tickets, /tickets/count/{eventId}");
	}
}
