package org.java.ticketingplatform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GeneralExceptionHandler {

	public ResponseEntity<String> handleTicketException(CreateTicketException ex){
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
}
