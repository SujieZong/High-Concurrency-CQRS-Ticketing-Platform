package org.java.purchaseservice.service.initialize;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.purchaseservice.config.EventConfig;
import org.java.purchaseservice.service.redis.SeatOccupiedService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * Initializes event configurations during application startup. Runs after
 * VenueConfigService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventConfigService {

  private final SeatOccupiedService seatService;
  private final EventConfig eventConfig;

  /**
   * Initializes events based on configuration. Skips if auto-initialize is
   * disabled.
   */
  public void initializeEvents() {
    try {
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
      int successfulInitializationCount = 0;
      int totalEvents = events.size();

      // Initialize all settings in the events list.
      for (EventConfig.Event event : events) {
        // only initiate enable = true
        if (event.isEnabled()) {
          try {
            // Log initialization data, count and total events and venue
            log.info(
                "[EventConfigService] Initializing event: {} for venue: {} ({}/{})",
                event.getEventId(),
                event.getVenueId(),
                successfulInitializationCount + 1,
                totalEvents);

            // Initialize Redis zones, bitmap, counters, metadata
            seatService.initializeAllZonesForEvent(event.getEventId(), event.getVenueId());

            successfulInitializationCount++;

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

      // Show initialization comparison: initialized vs total
      log.info(
          "[EventConfigService] Event initialization completed. Successfully initialized {}/{} events",
          successfulInitializationCount,
          totalEvents);
    } catch (Exception e) {
      log.error(
          "[EventConfigService] Critical error during event initialization: {}", e.getMessage(), e);
    }
  }
}
