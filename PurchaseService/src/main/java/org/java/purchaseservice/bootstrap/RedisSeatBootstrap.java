package org.java.purchaseservice.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.java.purchaseservice.service.redis.SeatOccupiedService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisSeatBootstrap implements ApplicationRunner {

	private final SeatOccupiedService seatOccupiedService;

	@Value("${tickets.bootstrap.venue-id:Venue1}")
	private String venueId;

	@Value("${tickets.bootstrap.event-id:Event1}")
	private String eventId;

	public RedisSeatBootstrap(SeatOccupiedService seatOccupiedService) {
		this.seatOccupiedService = seatOccupiedService;
	}


	@Override
	public void run(ApplicationArguments args) {
		log.info("[RedisSeatBootstrap] init seats: eventId={}, venueId={}", eventId, venueId);

		try {
			seatOccupiedService.initializeAllZonesForEvent(eventId, venueId);
			log.info("[RedisSeatBootstrap] finished");
		} catch (Exception e) {
			log.warn("[RedisSeatBootstrap] failed: {}", e.toString(), e);
		}
	}
}
