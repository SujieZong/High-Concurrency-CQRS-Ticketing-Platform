package org.java.ticketingplatform.exception;

public class ZoneFullException extends SeatOccupiedException {
	public ZoneFullException(String message) {
		super(message);
	}
}
