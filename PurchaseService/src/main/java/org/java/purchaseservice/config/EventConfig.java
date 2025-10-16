package org.java.purchaseservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Event configuration loaded from events.yml file.
 */
@Data
@Configuration
@Validated
@ConfigurationProperties(prefix = "events")
@PropertySource(value = "classpath:events.yml", factory = YamlPropertySourceFactory.class)
public class EventConfig {

    /**
     * Controls auto-initialization on startup. Defaults to false.
     */
    private boolean autoInitialize;

    /**
     * List of configured events.
     */
    @Valid
    private List<Event> list;

    /**
     * Single event configuration matching Event model structure.
     */
    @Data
    @Validated
    public static class Event {
        @NotBlank(message = "Event ID cannot be blank")
        private String eventId;

        @NotBlank(message = "Event name cannot be blank")
        private String name;

        @NotBlank(message = "Event type cannot be blank")
        private String type;

        @NotBlank(message = "Event date cannot be blank")
        private String date;

        @NotBlank(message = "Venue ID cannot be blank")
        private String venueId;

        /**
         * Event enabled status. Defaults to false if not specified.
         */
        private boolean enabled;
    }
}