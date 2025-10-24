package org.java.purchaseservice.service.initialize;

import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.java.purchaseservice.config.VenueConfig;
import org.java.purchaseservice.service.redis.RedisKeyUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Initializes venue configurations in Redis during startup.
 */
@Slf4j
@Service
@Order(1)
public class VenueConfigService implements InitializingBean {
  private final RedisTemplate<String, Object> redisTemplate;
  private final VenueConfig venueConfig;

  @Autowired
  public VenueConfigService(RedisTemplate<String, Object> redisTemplate, VenueConfig venueConfig) {
    this.redisTemplate = redisTemplate;
    this.venueConfig = venueConfig;
  }

  /**
   * Initializes all venues from configuration into Redis.
   * Called automatically after bean properties set.
   */
  @Override
  public void afterPropertiesSet() {
    log.info("[VenueConfigService] Starting venue initialization");

    // Read venue mapping from configuration
    Map<String, VenueConfig.Venue> venueMap = venueConfig.getMap();
    if (!CollectionUtils.isEmpty(venueMap)) {
      int venueCount = 0;
      // Initialization by iterate through each configured venue
      for (Map.Entry<String, VenueConfig.Venue> entry : venueMap.entrySet()) {
        String venueId = entry.getKey();
        VenueConfig.Venue venue = entry.getValue();
        try {
          // Initialize individual venue's zone
          initializeVenue(venueId, venue);
          venueCount++;
          log.debug("[VenueConfigService] Successfully initialized venue: {}", venueId);
        } catch (Exception e) {
          log.error(
              "[VenueConfigService] Failed to initialize venue: {}, error: {}",
              venueId,
              e.getMessage(),
              e);
        }
      }
      log.info(
          "[VenueConfigService] Initialized {}/{} venues from configuration",
          venueCount,
          venueMap.size());
    } else {
      log.warn("[VenueConfigService] No venues found in configuration");
    }

    try {
      // Backward compatibility default venue
      initializeBackwardCompatibilityVenue();
      log.info("[VenueConfigService] Backward compatibility venue initialized");
    } catch (Exception e) {
      log.error(
          "[VenueConfigService] Failed to initialize backward compatibility venue: {}",
          e.getMessage(),
          e);
    }

    log.info("[VenueConfigService] Venue initialization completed");
  }

  /** Initializes single venue's zone structure */
  private void initializeVenue(String venueId, VenueConfig.Venue venue) {
    // Get venue zone configuration parameters
    var zones = venue.getZones();

    int zoneCount = zones.getZoneCount();
    int rowCount = zones.getRowCount();
    int colCount = zones.getColCount();

    log.info(
        "[VenueConfigService] Initializing venue: {}, zones: {}, rows: {}, cols: {}",
        venueId,
        zoneCount,
        rowCount,
        colCount);

    // Create Redis data structures for each zone
    for (int zoneId = 1; zoneId <= zoneCount; zoneId++) {
      initializeVenueZone(venueId, zoneId, rowCount, colCount);
    }
  }

  /**
   * Initializes backward compatibility venue using default config.
   * Flow: Business Venue1 → Overridden by Technical Standard →
   * Final Venue1 Result: Venue1 = 100 zones × 26 rows × 30 cols
   * = 78,000 seats (standardized)
   */
  private void initializeBackwardCompatibilityVenue() {
    // Use default-config to create standard Venue1
    // OVERRIDES any previous Venue1 configuration from venues.map
    var defaultConfig = venueConfig.getDefaultConfig().getZones();
    String venueId = "Venue1";
    int zoneCount = defaultConfig.getZoneCount(); // 100 zones (standardized)
    int rowCount = defaultConfig.getRowCount(); // 26 rows (standardized)
    int colCount = defaultConfig.getColCount(); // 30 cols (standardized)

    log.info("[VenueConfigService] Initializing backward compatibility venue: {}", venueId);

    // Ensures Venue1 always has the same structure
    for (int zoneId = 1; zoneId <= zoneCount; zoneId++) {
      initializeVenueZone(venueId, zoneId, rowCount, colCount);
    }
  }

  /** Initializes a venue zone in Redis with capacity and structure metadata. */
  public void initializeVenueZone(String venueId, int zoneId, int rowCount, int colCount) {

    // row count in a zone for the venue
    String rowCountKey = RedisKeyUtil.getRowCountKey(venueId, zoneId);
    redisTemplate.opsForValue().set(rowCountKey, rowCount);

    // seat count for a zone for a row
    String seatPerRowKey = RedisKeyUtil.getSeatPerRowKey(venueId, zoneId);
    redisTemplate.opsForValue().set(seatPerRowKey, colCount); // Number of seats per row

    // get the capacity for zone
    String capacityKey = RedisKeyUtil.getZoneCapacityKey(venueId, zoneId);
    redisTemplate.opsForValue().set(capacityKey, rowCount * colCount); // Total zone capacity

    // get all zones in the set for venue
    String venueZonesKey = RedisKeyUtil.getZoneSetKey(venueId);
    redisTemplate.opsForSet().add(venueZonesKey, zoneId);
  }

  /**
   * Gets all zones for the specified venue.
   *
   * @param venueId the venue identifier
   * @return a set of zone IDs for the venue
   */
  public Set<Object> getVenueZones(String venueId) {
    String venueZonesKey = RedisKeyUtil.getZoneSetKey(venueId);
    return redisTemplate.opsForSet().members(venueZonesKey);
  }

  /**
   * Gets the row count for the specified venue zone.
   *
   * @param venueId the venue identifier
   * @param zoneId the zone identifier
   * @return the number of rows in the zone
   */
  public int getRowCount(String venueId, int zoneId) {
    String rowCountKey = RedisKeyUtil.getRowCountKey(venueId, zoneId);
    return getIntValue(rowCountKey);
  }

  /**
   * Gets the number of seats per row for the specified venue zone.
   *
   * @param venueId the venue identifier
   * @param zoneId the zone identifier
   * @return the number of seats per row in the zone
   */
  public int getSeatPerRow(String venueId, int zoneId) {
    String seatKey = RedisKeyUtil.getSeatPerRowKey(venueId, zoneId);
    return getIntValue(seatKey);
  }

  /**
   * Returns the configured capacity (total number of seats) for the given venue zone.
   *
   * @param venueId the venue identifier
   * @param zoneId the zone identifier
   * @return the total capacity of the zone
   */
  public int getZoneCapacity(String venueId, int zoneId) {
    String zoneKey = RedisKeyUtil.getZoneCapacityKey(venueId, zoneId);
    return getIntValue(zoneKey);
  }

  private int getIntValue(String key) {
    Object value = redisTemplate.opsForValue().get(key);
    if (value != null) {
      try {
        return Integer.parseInt(value.toString());
      } catch (NumberFormatException e) {
        return 0;
      }
    }
    return 0;
  }
}
