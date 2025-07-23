package org.java.ticketingplatform.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.ticketingplatform.service.initialize.VenueConfigService;
import org.java.ticketingplatform.service.redis.SeatOccupiedService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventSeatBootstrap implements ApplicationRunner {

	private final VenueConfigService venueConfigService;
	private final SeatOccupiedService seatService;
	private final JdbcTemplate jdbcTemplate;

	private void initVenue() {
		String venueId = "Venue1";
		String city = "Vancouver";
		jdbcTemplate.update("""
				  INSERT INTO venue(venue_id, city) VALUES(?, ?)
				  				ON DUPLICATE KEY UPDATE city = VALUES(city)
				""", venueId, city);
	}

	private void initEvent() {
		String venueId = "Venue1";
		String eventId = "Event1";
		var name = "The concert that interests everyone";
		var type = "concert";
		var eventDate = LocalDate.of(2030, 1, 1);

		jdbcTemplate.update("""
				  INSERT INTO event(event_id, venue_id, name, type, event_date)
				  VALUES(?, ?, ?, ?, ?)
				  ON DUPLICATE KEY UPDATE name=name
				""", eventId, venueId, name, type, eventDate);
	}

	private void initZone() {
		String venueId = "Venue1";

		for (int zone = 1; zone <= 100; zone++) {
			int group = (zone - 1) / 20;         // 0..4
			BigDecimal price = BigDecimal.valueOf(20).multiply(
					BigDecimal.valueOf(2).pow(4 - group)
			);

			jdbcTemplate.update("""
					  INSERT INTO zone(venue_Id, zone_id, ticket_price, row_count, col_count)
					  VALUES(?, ?, ?, ?, ?)
					  ON DUPLICATE KEY UPDATE ticket_price = VALUES(ticket_price)
					""", venueId, zone, price, 26, 30);
		}
	}


	@Override
	public void run(ApplicationArguments args) {
		initVenue();
		initEvent();
		initZone();

		String venueId = "Venue1";
		String eventId = "Event1";

		for (Object zone : venueConfigService.getVenueZones(venueId)) {
			int zoneId = Integer.parseInt(zone.toString());
			seatService.initializeEventSeat(eventId, venueId, zoneId);
			log.trace(" Initialized zone {}", zoneId);
		}
		log.trace(">>> [EventSeatBootstrap] Initialization Finished");
	}
}
