package org.java.queryservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GeneralExceptionHandler {

	@ExceptionHandler(TicketNotFoundException.class)
	public ResponseEntity<String> handleTicketNotFound(TicketNotFoundException ex) {
		String errorMessage = ex.getMessage();
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<String> handleMissingParameter(MissingServletRequestParameterException ex) {
		String name = ex.getParameterName();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Missing required parameter: " + name);
	}

	@ExceptionHandler(ZoneFullException.class)
	public ResponseEntity<String> handleZoneFull(ZoneFullException ex) {
		String errorMessage = "Zone Full: " + ex.getMessage();
		return ResponseEntity.status(HttpStatus.CONFLICT).body("Redis Error--" + errorMessage);
	}

	@ExceptionHandler(RowFullException.class)
	public ResponseEntity<String> handleRowFull(RowFullException ex) {
		String errorMessage = "Row Full: " + ex.getMessage();
		return ResponseEntity.status(HttpStatus.CONFLICT).body("Redis Error--" + errorMessage);
	}

	@ExceptionHandler(SeatOccupiedException.class)
	public ResponseEntity<String> handleSeatOccupied(SeatOccupiedException ex) {
		String errorMessage = "Seat Occupied: " + ex.getMessage();
		return ResponseEntity.status(HttpStatus.CONFLICT).body("Redis Error--" + errorMessage);
	}

	/**
	 * Handle 404 Not Found errors - simplified but informative
	 */
	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(NoHandlerFoundException ex) {
		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put("error", "Endpoint Not Found");
		errorResponse.put("message", "The requested endpoint does not exist");
		errorResponse.put("path", ex.getRequestURL());
		errorResponse.put("availableEndpoints", new String[] {
				"GET /api/v1/health",
				"GET /api/v1/tickets/{ticketId}",
				"GET /api/v1/tickets/tickets",
				"GET /api/v1/tickets/count/{eventId}",
				"GET /api/v1/tickets/revenue/{venueId}/{eventId}"
		});

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
	}

}
