package org.java.purchaseservice.service.initialize;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.purchaseservice.config.EventConfig;
import org.java.purchaseservice.service.redis.SeatOccupiedService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/** Initializes event configurations during application startup. Runs after VenueConfigService */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class EventConfigService implements ApplicationRunner {
  private final SeatOccupiedService seatService;
  private final EventConfig eventConfig;

  /** Initializes events based on configuration. Skips if auto-initialize is disabled. */
  @Override
  public void run(ApplicationArguments args) {

    // If auto initialize false, skip everything
    if (!eventConfig.isAutoInitialize()) {
      log.info("[EventConfigService] Auto-initialize is disabled, skipping event initialization");
      return;
    }

    log.info("[EventConfigService] Starting event initialization");

    // Get events list from YML
    List<EventConfig.Event> events = eventConfig.getList();
    if (CollectionUtils.isEmpty(events)) {
      log.warn("[EventConfigService] No events configured for initialization");
      return;
    }

    // counters and event size count
    int initializedCount = 0;
    int totalEvents = events.size();

    // Initiate all setting in the events list.
    for (EventConfig.Event event : events) {
      // only initiate enable = true
      if (event.isEnabled()) {
        try {
          // Log Initilization data, count and total events and venue
          log.info(
              "[EventConfigService] Initializing event: {} for venue: {} ({}/{})",
              event.getEventId(),
              event.getVenueId(),
              initializedCount + 1,
              totalEvents);

          // Initiate Redis Zones, bitmap, counters, metadata
          seatService.initializeAllZonesForEvent(event.getEventId(), event.getVenueId());

          // counter ++
          initializedCount++;

          log.info("[EventConfigService] Successfully initialized event: {}", event.getEventId());
        } catch (Exception e) {
          // Error handling
          log.error(
              "[EventConfigService] Failed to initialize event: {}, error: {}",
              event.getEventId(),
              e.getMessage(),
              e);
        }
      } else {
        // Skip disabled event
        log.info("[EventConfigService] Skipping disabled event: {}", event.getEventId());
      }
    }

    // show initialization comparison initialized vs total
    log.info(
        "[EventConfigService] Event initialization completed. Successfully initialized {}/{} events",
        initializedCount,
        totalEvents);
  }
}
