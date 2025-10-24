package org.java.purchaseservice.service.initialize;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Orchestrates startup initialization: - Venue first - Event second
 *
 * <p>Keeps business logic inside VenueConfigService / EventConfigService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class CombinedInitializer implements ApplicationRunner {

  private final VenueConfigService venueConfigService;
  private final EventConfigService eventConfigService;

  @Override
  public void run(ApplicationArguments args) {
    log.info("[CombinedInitializer] BEGIN");

    try {
      log.info("[CombinedInitializer] Running venue initialization...");
      venueConfigService.initializeVenues();
      log.info("[CombinedInitializer] Venue initialization done.");
    } catch (Exception e) {
      log.error("[CombinedInitializer] Venue initialization failed: {}", e.getMessage(), e);
    }

    try {
      log.info("[CombinedInitializer] Running event initialization...");
      eventConfigService.initializeEvents();
      log.info("[CombinedInitializer] Event initialization done.");
    } catch (Exception e) {
      log.error("[CombinedInitializer] Event initialization failed: {}", e.getMessage(), e);
    }

    log.info("[CombinedInitializer] END");
  }
}
