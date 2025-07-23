package org.java.ticketingplatform.exception;

public class RowFullException extends SeatOccupiedException {
	public RowFullException(String message) {
		super(message);
	}
}
