package org.java.mqprojectionservice.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * MySQL Data Initialization Bootstrap
 * Ensures that venue/zone configurations in MySQL are consistent with Redis
 * configurations in PurchaseService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MySQLDataBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("[MySQLDataBootstrap] Starting MySQL data initialization...");

        try {
            initializeVenueData();
            initializeEventData();
            log.info("[MySQLDataBootstrap] MySQL data initialization completed successfully!");
        } catch (Exception e) {
            log.error("[MySQLDataBootstrap] Failed to initialize MySQL data: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Initialize venue and zone data to maintain consistency with PurchaseService
     */
    private void initializeVenueData() {
        // 1. Initialize main venue
        String venueId = "Venue1";
        String city = "Shanghai";

        log.info("[MySQLDataBootstrap] Initializing venue: {}", venueId);

        // Insert venue (if not exists)
        String venueSQL = """
                INSERT INTO venue (venue_id, city)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE city = VALUES(city)
                """;
        jdbcTemplate.update(venueSQL, venueId, city);

        // 2. Initialize 100 zones, consistent with PurchaseService's VenueConfigService
        int zoneCount = 100;
        int rowCount = 26; // Consistent with PurchaseService
        int colCount = 30; // Consistent with PurchaseService

        log.info("[MySQLDataBootstrap] Initializing {} zones for venue {}", zoneCount, venueId);

        String zoneSQL = """
                INSERT INTO zone (venue_id, zone_id, ticket_price, row_count, col_count)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    ticket_price = VALUES(ticket_price),
                    row_count = VALUES(row_count),
                    col_count = VALUES(col_count)
                """;

        for (int zoneId = 1; zoneId <= zoneCount; zoneId++) {
            // Differentiated pricing strategy
            BigDecimal ticketPrice;
            if (zoneId <= 10) {
                ticketPrice = new BigDecimal("200.00"); // VIP area
            } else if (zoneId <= 30) {
                ticketPrice = new BigDecimal("150.00"); // Premium area
            } else {
                ticketPrice = new BigDecimal("100.00"); // General area
            }

            jdbcTemplate.update(zoneSQL, venueId, zoneId, ticketPrice, rowCount, colCount);
        }

        // 3. Insert test venue
        jdbcTemplate.update(venueSQL, "venue-test", "Vancouver");
        jdbcTemplate.update(zoneSQL, "venue-test", 1, new BigDecimal("80.00"), 12, 25);

        log.info("[MySQLDataBootstrap] Venue and zone initialization completed");
    }

    /**
     * Initialize event data
     */
    private void initializeEventData() {
        log.info("[MySQLDataBootstrap] Initializing event data...");

        String eventSQL = """
                INSERT INTO event (event_id, venue_id, name, type, event_date)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    type = VALUES(type),
                    event_date = VALUES(event_date)
                """;

        // Main test event
        jdbcTemplate.update(eventSQL,
                "Event1", "Venue1", "Spring Concert 2025", "Concert", "2025-12-25");

        // Test event
        jdbcTemplate.update(eventSQL,
                "event-test", "venue-test", "Test Event", "Test", "2025-11-15");

        log.info("[MySQLDataBootstrap] Event initialization completed");
    }
}