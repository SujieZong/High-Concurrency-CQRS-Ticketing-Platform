package org.java.purchaseservice.service.initialize;

import org.java.purchaseservice.config.EventConfig;
import org.java.purchaseservice.config.VenueConfig;
import org.java.purchaseservice.service.redis.SeatOccupiedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for the initialization flow.
 * Tests the execution order: VenueConfigService → EventConfigService
 */
@ExtendWith(MockitoExtension.class)
class InitializationFlowIntegrationTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private SetOperations<String, Object> setOperations;
    @Mock
    private VenueConfig venueConfig;
    @Mock
    private EventConfig eventConfig;
    @Mock
    private SeatOccupiedService seatOccupiedService;
    @Mock
    private ApplicationArguments applicationArguments;

    private VenueConfigService venueConfigService;
    private EventConfigService eventConfigService;

    @BeforeEach
    void setUp() {
        // Mock Redis operations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        // Initialize services
        venueConfigService = new VenueConfigService(redisTemplate, venueConfig);
        eventConfigService = new EventConfigService(seatOccupiedService, eventConfig);
    }

    @Test
    void completeInitializationFlow_executesInCorrectOrder() {
        // Given: Configure venue and event data
        setupVenueConfiguration();
        setupEventConfiguration();

        // When: Execute initialization flow in order
        // Step 1: Initialize venues (@Order(1))
        venueConfigService.afterPropertiesSet();

        // Step 2: Initialize events (@Order(2))
        eventConfigService.run(applicationArguments);

        // Then: Verify execution order and interactions
        InOrder inOrder = inOrder(redisTemplate, seatOccupiedService);

        // First: Venue initialization should set up Redis structure
        inOrder.verify(redisTemplate, atLeastOnce()).opsForValue();
        inOrder.verify(redisTemplate, atLeastOnce()).opsForSet();

        // Second: Event initialization should call seat service
        inOrder.verify(seatOccupiedService).initializeAllZonesForEvent("EVENT001", "Venue1");
        inOrder.verify(seatOccupiedService).initializeAllZonesForEvent("EVENT002", "Venue2");
    }

    @Test
    void venueInitializationFailure_doesNotAffectEventInitialization() {
        // Given: Venue initialization will fail, but event config is valid
        when(venueConfig.getMap()).thenThrow(new RuntimeException("Venue config error"));
        setupEventConfiguration();

        // When: Execute initialization flow
        assertDoesNotThrow(() -> venueConfigService.afterPropertiesSet());
        assertDoesNotThrow(() -> eventConfigService.run(applicationArguments));

        // Then: Event initialization should still proceed
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT001", "Venue1");
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT002", "Venue2");
    }

    @Test
    void eventInitializationFailure_handledGracefully() {
        // Given: Event initialization will fail
        setupVenueConfiguration();
        when(eventConfig.isAutoInitialize()).thenReturn(true);
        when(eventConfig.getList()).thenThrow(new RuntimeException("Event config error"));

        // When: Execute initialization flow
        assertDoesNotThrow(() -> venueConfigService.afterPropertiesSet());
        assertDoesNotThrow(() -> eventConfigService.run(applicationArguments));

        // Then: Venue initialization should complete successfully
        verify(redisTemplate, atLeastOnce()).opsForValue();
    }

    @Test
    void backwardCompatibilityVenue_overridesVenue1Configuration() {
        // Given: Venue1 in map and default config
        Map<String, VenueConfig.Venue> venueMap = new HashMap<>();
        VenueConfig.Venue venue1 = mock(VenueConfig.Venue.class);
        VenueConfig.ZoneSettings customZones = mock(VenueConfig.ZoneSettings.class);
        VenueConfig.DefaultSettings defaultSettings = mock(VenueConfig.DefaultSettings.class);
        VenueConfig.ZoneSettings defaultZones = mock(VenueConfig.ZoneSettings.class);

        venueMap.put("Venue1", venue1);
        when(venueConfig.getMap()).thenReturn(venueMap);
        when(venue1.getZones()).thenReturn(customZones);
        when(customZones.getZoneCount()).thenReturn(50);
        when(customZones.getRowCount()).thenReturn(30);
        when(customZones.getColCount()).thenReturn(40);

        when(venueConfig.getDefaultConfig()).thenReturn(defaultSettings);
        when(defaultSettings.getZones()).thenReturn(defaultZones);
        when(defaultZones.getZoneCount()).thenReturn(100);
        when(defaultZones.getRowCount()).thenReturn(26);
        when(defaultZones.getColCount()).thenReturn(30);

        // When: Initialize venues
        venueConfigService.afterPropertiesSet();

        // Then: Venue1 should be initialized twice (custom + default override)
        verify(valueOperations, times(50)).set(contains("Venue1:zone:"), eq(1200));
        verify(valueOperations, times(100)).set(contains("Venue1:zone:"), eq(780));
    }

    @Test
    void fullStackInitialization_withMultipleVenuesAndEvents() {
        // Given: Complex configuration with multiple venues and events
        setupComplexConfiguration();

        // When: Execute complete initialization flow
        venueConfigService.afterPropertiesSet();
        eventConfigService.run(applicationArguments);

        // Then: Verify all venues and events are initialized
        verify(valueOperations, atLeast(180)).set(anyString(), anyInt());
        verify(setOperations, atLeast(180)).add(anyString(), anyInt());

        // Events: 3 enabled events
        verify(seatOccupiedService, times(3)).initializeAllZonesForEvent(anyString(), anyString());
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT001", "Venue1");
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT003", "Venue3");
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT005", "Venue2");
    }

    @Test
    void autoInitializeDisabled_onlyVenuesExecute() {
        // Given: Auto-initialize is disabled for events
        setupVenueConfiguration();
        when(eventConfig.isAutoInitialize()).thenReturn(false);

        // When: Execute initialization flow
        venueConfigService.afterPropertiesSet();
        eventConfigService.run(applicationArguments);

        // Then: Only venue initialization should occur
        verify(redisTemplate, atLeastOnce()).opsForValue();
        verify(seatOccupiedService, never()).initializeAllZonesForEvent(anyString(), anyString());
    }

    private void setupVenueConfiguration() {
        Map<String, VenueConfig.Venue> venueMap = new HashMap<>();
        VenueConfig.Venue venue1 = mock(VenueConfig.Venue.class);
        VenueConfig.ZoneSettings zones1 = mock(VenueConfig.ZoneSettings.class);

        venueMap.put("Venue1", venue1);
        when(venueConfig.getMap()).thenReturn(venueMap);
        when(venue1.getZones()).thenReturn(zones1);
        when(zones1.getZoneCount()).thenReturn(50);
        when(zones1.getRowCount()).thenReturn(30);
        when(zones1.getColCount()).thenReturn(40);

        // Default configuration
        VenueConfig.DefaultSettings defaultSettings = mock(VenueConfig.DefaultSettings.class);
        VenueConfig.ZoneSettings defaultZones = mock(VenueConfig.ZoneSettings.class);
        when(venueConfig.getDefaultConfig()).thenReturn(defaultSettings);
        when(defaultSettings.getZones()).thenReturn(defaultZones);
        when(defaultZones.getZoneCount()).thenReturn(100);
        when(defaultZones.getRowCount()).thenReturn(26);
        when(defaultZones.getColCount()).thenReturn(30);
    }

    private void setupEventConfiguration() {
        when(eventConfig.isAutoInitialize()).thenReturn(true);

        EventConfig.Event event1 = createEvent("EVENT001", "Concert A", "Venue1", true);
        EventConfig.Event event2 = createEvent("EVENT002", "Concert B", "Venue2", true);

        when(eventConfig.getList()).thenReturn(List.of(event1, event2));
    }

    private void setupComplexConfiguration() {
        // Complex venue setup
        Map<String, VenueConfig.Venue> venueMap = new HashMap<>();

        // Venue1: 50 zones
        VenueConfig.Venue venue1 = mock(VenueConfig.Venue.class);
        VenueConfig.ZoneSettings zones1 = mock(VenueConfig.ZoneSettings.class);
        venueMap.put("Venue1", venue1);
        when(venue1.getZones()).thenReturn(zones1);
        when(zones1.getZoneCount()).thenReturn(50);
        when(zones1.getRowCount()).thenReturn(30);
        when(zones1.getColCount()).thenReturn(40);

        // Venue2: 60 zones
        VenueConfig.Venue venue2 = mock(VenueConfig.Venue.class);
        VenueConfig.ZoneSettings zones2 = mock(VenueConfig.ZoneSettings.class);
        venueMap.put("Venue2", venue2);
        when(venue2.getZones()).thenReturn(zones2);
        when(zones2.getZoneCount()).thenReturn(60);
        when(zones2.getRowCount()).thenReturn(25);
        when(zones2.getColCount()).thenReturn(35);

        // Venue3: 70 zones
        VenueConfig.Venue venue3 = mock(VenueConfig.Venue.class);
        VenueConfig.ZoneSettings zones3 = mock(VenueConfig.ZoneSettings.class);
        venueMap.put("Venue3", venue3);
        when(venue3.getZones()).thenReturn(zones3);
        when(zones3.getZoneCount()).thenReturn(70);
        when(zones3.getRowCount()).thenReturn(20);
        when(zones3.getColCount()).thenReturn(30);

        when(venueConfig.getMap()).thenReturn(venueMap);

        // Default configuration
        VenueConfig.DefaultSettings defaultSettings = mock(VenueConfig.DefaultSettings.class);
        VenueConfig.ZoneSettings defaultZones = mock(VenueConfig.ZoneSettings.class);
        when(venueConfig.getDefaultConfig()).thenReturn(defaultSettings);
        when(defaultSettings.getZones()).thenReturn(defaultZones);
        when(defaultZones.getZoneCount()).thenReturn(100);
        when(defaultZones.getRowCount()).thenReturn(26);
        when(defaultZones.getColCount()).thenReturn(30);

        // Complex event setup
        when(eventConfig.isAutoInitialize()).thenReturn(true);

        List<EventConfig.Event> events = List.of(
            createEvent("EVENT001", "Concert A", "Venue1", true),   // enabled
            createEvent("EVENT002", "Concert B", "Venue1", false),  // disabled
            createEvent("EVENT003", "Concert C", "Venue3", true),   // enabled
            createEvent("EVENT004", "Concert D", "Venue2", false),  // disabled
            createEvent("EVENT005", "Concert E", "Venue2", true)    // enabled
        );

        when(eventConfig.getList()).thenReturn(events);
    }

    private EventConfig.Event createEvent(String eventId, String name, String venueId, boolean enabled) {
        EventConfig.Event event = new EventConfig.Event();
        event.setEventId(eventId);
        event.setName(name);
        event.setType("CONCERT");
        event.setDate("2024-12-01");
        event.setVenueId(venueId);
        event.setEnabled(enabled);
        return event;
    }
}
