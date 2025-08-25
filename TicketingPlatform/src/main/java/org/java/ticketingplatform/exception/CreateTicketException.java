package org.java.ticketingplatform.exception;

public class CreateTicketException extends RuntimeException {
	public CreateTicketException(String message) {
		super(message);
	}

	public CreateTicketException(String message, Throwable cause) {
		super(message, cause);
	}
}
