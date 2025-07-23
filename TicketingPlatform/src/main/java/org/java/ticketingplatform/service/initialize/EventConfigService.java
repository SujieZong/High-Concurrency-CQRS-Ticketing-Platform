package org.java.ticketingplatform.service.initialize;

import lombok.RequiredArgsConstructor;
import org.java.ticketingplatform.service.redis.SeatOccupiedService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventConfigService implements ApplicationRunner {
	private final SeatOccupiedService seatService;

	@Override
	public void run(ApplicationArguments args) {
		String venueId = "Venue1";
		String eventId = "Event1";

		seatService.initializeAllZonesForEvent(eventId, venueId);
	}
}
