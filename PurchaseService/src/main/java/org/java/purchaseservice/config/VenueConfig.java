package org.java.purchaseservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/**
 * Configuration for venue management with Map-based O(1) lookup.
 * Loads from application.yml venues section.
 */
@Data
@Configuration
@Validated
@ConfigurationProperties(prefix = "venues")
public class VenueConfig {

    /**
     * Map of venues indexed by venue ID for O(1) lookup.
     */
    @NotEmpty(message = "Venues map cannot be empty")
    @Valid
    private Map<String, Venue> map;

    /**
     * Default settings for backward compatibility.
     */
    @Valid
    private DefaultSettings defaultConfig;

    /**
     * Single venue configuration matching Venue model structure.
     */
    @Data
    @Validated
    public static class Venue {
        @Valid
        private ZoneSettings zones;
    }

    /**
     * Default configuration for backward compatibility.
     */
    @Data
    @Validated
    public static class DefaultSettings {
        @Valid
        private ZoneSettings zones;
    }

    /**
     * Zone configuration matching Zone model structure (simplified for config).
     */
    @Data
    @Validated
    public static class ZoneSettings {
        @Min(value = 1, message = "Zone count must be at least 1")
        private int zoneCount;

        @Min(value = 1, message = "Row count must be at least 1")
        private int rowCount;

        @Min(value = 1, message = "Column count must be at least 1")
        private int colCount;
    }
}