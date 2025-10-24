package org.java.purchaseservice.service.initialize;

import org.java.purchaseservice.config.VenueConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for VenueConfigService initialization process.
 * Verifies that venue configurations are properly loaded into Redis.
 */
@ExtendWith(MockitoExtension.class)
class VenueConfigServiceTest {

        @Mock
        private RedisTemplate<String, Object> redisTemplate;

        @Mock
        private ValueOperations<String, Object> valueOperations;

        @Mock
        private SetOperations<String, Object> setOperations;

        @Mock
        private VenueConfig venueConfig;

        @Mock
        private VenueConfig.Venue venue1;

        @Mock
        private VenueConfig.Venue venue2;

        @Mock
        private VenueConfig.ZoneSettings zoneSettings1;

        @Mock
        private VenueConfig.ZoneSettings zoneSettings2;

        @Mock
        private VenueConfig.DefaultSettings defaultSettings;

        @Mock
        private VenueConfig.ZoneSettings defaultZoneSettings;

        @Captor
        private ArgumentCaptor<String> keyCaptor;

        @Captor
        private ArgumentCaptor<Object> valueCaptor;

        private VenueConfigService venueConfigService;

        @BeforeEach
        void setUp() {
                // Mock Redis operations - only set up the basic mocks needed for all tests
                lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

                venueConfigService = new VenueConfigService(redisTemplate, venueConfig);
        }

        @Test
        void initializeVenues_initializesConfiguredVenues_success() {
                // Given: Configure venue map with two venues
                Map<String, VenueConfig.Venue> venueMap = new HashMap<>();
                venueMap.put("Venue1", venue1);
                venueMap.put("Venue2", venue2);

                // Venue1 configuration: 50 zones, 30 rows, 40 cols
                when(venue1.getZones()).thenReturn(zoneSettings1);
                when(zoneSettings1.getZoneCount()).thenReturn(50);
                when(zoneSettings1.getRowCount()).thenReturn(30);
                when(zoneSettings1.getColCount()).thenReturn(40);

                // Venue2 configuration: 80 zones, 25 rows, 35 cols
                when(venue2.getZones()).thenReturn(zoneSettings2);
                when(zoneSettings2.getZoneCount()).thenReturn(80);
                when(zoneSettings2.getRowCount()).thenReturn(25);
                when(zoneSettings2.getColCount()).thenReturn(35);

                // Default config: 100 zones, 26 rows, 30 cols
                when(venueConfig.getMap()).thenReturn(venueMap);
                when(venueConfig.getDefaultConfig()).thenReturn(defaultSettings);
                when(defaultSettings.getZones()).thenReturn(defaultZoneSettings);
                when(defaultZoneSettings.getZoneCount()).thenReturn(100);
                when(defaultZoneSettings.getRowCount()).thenReturn(26);
                when(defaultZoneSettings.getColCount()).thenReturn(30);

                // When: Initialize venues
                venueConfigService.initializeVenues();

                // Then: Verify venue initializations
                // Venue1: 50 zones × (30 rows × 40 cols) = 50 zones × 1200 seats
                verify(valueOperations, times(50))
                                .set(argThat(key -> key.contains("venue:Venue1:zone:") && key.contains(":capacity")),
                                                eq(1200));
                verify(valueOperations, times(50)).set(
                                argThat(key -> key.contains("venue:Venue1:zone:") && key.contains(":rowCount")),
                                eq(30));
                verify(valueOperations, times(50)).set(
                                argThat(key -> key.contains("venue:Venue1:zone:") && key.contains(":seatPerRow")),
                                eq(40));
                verify(setOperations, times(150)).add(eq("venue:Venue1"), any()); // 50 + 100 = 150 total

                // Venue2: 80 zones × (25 rows × 35 cols) = 80 zones × 875 seats
                verify(valueOperations, times(80))
                                .set(argThat(key -> key.contains("venue:Venue2:zone:") && key.contains(":capacity")),
                                                eq(875));
                verify(valueOperations, times(80)).set(
                                argThat(key -> key.contains("venue:Venue2:zone:") && key.contains(":rowCount")),
                                eq(25));
                verify(valueOperations, times(80)).set(
                                argThat(key -> key.contains("venue:Venue2:zone:") && key.contains(":seatPerRow")),
                                eq(35));
                verify(setOperations, times(80)).add(eq("venue:Venue2"), any());

                // Backward compatibility Venue1 override: 100 zones × (26 rows × 30 cols) = 100
                // zones × 780 seats
                verify(valueOperations, times(100))
                                .set(argThat(key -> key.contains("venue:Venue1:zone:") && key.contains(":capacity")),
                                                eq(780));
                verify(valueOperations, times(100)).set(
                                argThat(key -> key.contains("venue:Venue1:zone:") && key.contains(":rowCount")),
                                eq(26));
                verify(valueOperations, times(100)).set(
                                argThat(key -> key.contains("venue:Venue1:zone:") && key.contains(":seatPerRow")),
                                eq(30));
                verify(setOperations, times(150)).add(eq("venue:Venue1"), any()); // 50 + 100 = 150 total
        }

        @Test
        void initializeVenues_emptyVenueMap_onlyInitializesDefaultVenue() {
                // Given: Empty venue map
                when(venueConfig.getMap()).thenReturn(new HashMap<>());
                when(venueConfig.getDefaultConfig()).thenReturn(defaultSettings);
                when(defaultSettings.getZones()).thenReturn(defaultZoneSettings);
                when(defaultZoneSettings.getZoneCount()).thenReturn(100);
                when(defaultZoneSettings.getRowCount()).thenReturn(26);
                when(defaultZoneSettings.getColCount()).thenReturn(30);

                // When: Initialize venues
                venueConfigService.initializeVenues();

                // Then: Only default Venue1 should be initialized
                verify(valueOperations, times(100))
                                .set(argThat(key -> key.contains("venue:Venue1:zone:") && key.contains(":capacity")),
                                                eq(780));
                verify(setOperations, times(100)).add(eq("venue:Venue1"), any());
        }

        @Test
        void initializeVenues_venueInitializationFailure_continuesWithOtherVenues() {
                // Given: Venue map with one valid and one problematic venue
                Map<String, VenueConfig.Venue> venueMap = new HashMap<>();
                venueMap.put("ValidVenue", venue1);
                venueMap.put("ProblematicVenue", venue2);

                // Valid venue configuration
                when(venue1.getZones()).thenReturn(zoneSettings1);
                when(zoneSettings1.getZoneCount()).thenReturn(10);
                when(zoneSettings1.getRowCount()).thenReturn(20);
                when(zoneSettings1.getColCount()).thenReturn(30);

                // Problematic venue - throws exception
                when(venue2.getZones()).thenThrow(new RuntimeException("Configuration error"));

                // Default configuration
                when(venueConfig.getMap()).thenReturn(venueMap);
                when(venueConfig.getDefaultConfig()).thenReturn(defaultSettings);
                when(defaultSettings.getZones()).thenReturn(defaultZoneSettings);
                when(defaultZoneSettings.getZoneCount()).thenReturn(5);
                when(defaultZoneSettings.getRowCount()).thenReturn(10);
                when(defaultZoneSettings.getColCount()).thenReturn(15);

                // When: Initialize venues (should not throw exception)
                assertDoesNotThrow(() -> venueConfigService.initializeVenues());

                // Then: ValidVenue should be initialized, ProblematicVenue skipped, default
                // venue initialized
                verify(valueOperations, times(10))
                                .set(argThat(key -> key.contains("ValidVenue:zone:") && key.contains(":capacity")),
                                                eq(600)); // 20×30=600
                verify(valueOperations, times(5)).set(
                                argThat(key -> key.contains("venue:Venue1:zone:") && key.contains(":capacity")),
                                eq(150)); // 10×15=150 (default)
        }

        @Test
        void initializeVenueZone_createsCorrectRedisStructures() {
                // Given: Venue zone parameters
                String venueId = "TestVenue";
                int zoneId = 5;
                int rowCount = 25;
                int colCount = 30;

                // When: Initialize single venue zone
                venueConfigService.initializeVenueZone(venueId, zoneId, rowCount, colCount);

                // Then: Verify correct Redis keys and values are set
                verify(valueOperations).set("venue:TestVenue:zone:5:rowCount", 25);
                verify(valueOperations).set("venue:TestVenue:zone:5:seatPerRow", 30);
                verify(valueOperations).set("venue:TestVenue:zone:5:capacity", 750); // 25×30=750
                verify(setOperations).add("venue:TestVenue", 5);
        }

        @Test
        void getVenueZones_returnsRedisSetMembers() {
                // Given: Mock Redis set members
                Set<Object> expectedZones = Set.of(1, 2, 3, 4, 5);
                when(setOperations.members("venue:TestVenue")).thenReturn(expectedZones);

                // When: Get venue zones
                Set<Object> actualZones = venueConfigService.getVenueZones("TestVenue");

                // Then: Should return the mocked set
                assertEquals(expectedZones, actualZones);
                verify(setOperations).members("venue:TestVenue");
        }

        @Test
        void getRowCount_returnsCorrectValue() {
                // Given: Mock Redis value
                when(valueOperations.get("venue:TestVenue:zone:1:rowCount")).thenReturn("25");

                // When: Get row count
                int rowCount = venueConfigService.getRowCount("TestVenue", 1);

                // Then: Should return parsed integer
                assertEquals(25, rowCount);
        }

        @Test
        void getSeatPerRow_returnsCorrectValue() {
                // Given: Mock Redis value
                when(valueOperations.get("venue:TestVenue:zone:1:seatPerRow")).thenReturn("30");

                // When: Get seat per row
                int seatPerRow = venueConfigService.getSeatPerRow("TestVenue", 1);

                // Then: Should return parsed integer
                assertEquals(30, seatPerRow);
        }

        @Test
        void getZoneCapacity_returnsCorrectValue() {
                // Given: Mock Redis value
                when(valueOperations.get("venue:TestVenue:zone:1:capacity")).thenReturn("750");

                // When: Get zone capacity
                int capacity = venueConfigService.getZoneCapacity("TestVenue", 1);

                // Then: Should return parsed integer
                assertEquals(750, capacity);
        }

        @Test
        void getIntValue_handlesNullValue_returnsZero() {
                // Given: Redis returns null for the actual key that will be called
                when(valueOperations.get("venue:NonExistent:zone:999:capacity")).thenReturn(null);

                // When: Get value for nonexistent key
                int value = venueConfigService.getZoneCapacity("NonExistent", 999);

                // Then: Should return 0 for null value
                assertEquals(0, value);
        }

        @Test
        void getIntValue_handlesInvalidFormat_returnsZero() {
                // Given: Redis returns invalid number format
                when(valueOperations.get("venue:TestVenue:zone:1:capacity")).thenReturn("invalid-number");

                // When: Get value with invalid format
                int value = venueConfigService.getZoneCapacity("TestVenue", 1);

                // Then: Should return 0 for invalid format
                assertEquals(0, value);
        }

        @Test
        void backwardCompatibilityVenue_overridesPreviousVenue1Configuration() {
                // Given: Venue1 exists in map with custom config, and default config
                Map<String, VenueConfig.Venue> venueMap = new HashMap<>();
                venueMap.put("Venue1", venue1);

                // Custom Venue1: 50 zones, 30 rows, 40 cols
                when(venue1.getZones()).thenReturn(zoneSettings1);
                when(zoneSettings1.getZoneCount()).thenReturn(50);
                when(zoneSettings1.getRowCount()).thenReturn(30);
                when(zoneSettings1.getColCount()).thenReturn(40);

                // Default config: 100 zones, 26 rows, 30 cols (should override)
                when(venueConfig.getMap()).thenReturn(venueMap);
                when(venueConfig.getDefaultConfig()).thenReturn(defaultSettings);
                when(defaultSettings.getZones()).thenReturn(defaultZoneSettings);
                when(defaultZoneSettings.getZoneCount()).thenReturn(100);
                when(defaultZoneSettings.getRowCount()).thenReturn(26);
                when(defaultZoneSettings.getColCount()).thenReturn(30);

                // When: Initialize venues
                venueConfigService.initializeVenues();

                // Then: Venue1 should be initialized twice
                // First: Custom config (50 zones × 1200 capacity)
                // Second: Default config override (100 zones × 780 capacity)
                // The final result should be the default config values
                verify(valueOperations, times(50))
                                .set(argThat(key -> key.contains("venue:Venue1:zone:") && key.contains(":capacity")),
                                                eq(1200)); // custom
                                                           // config
                verify(valueOperations, times(100))
                                .set(argThat(key -> key.contains("venue:Venue1:zone:") && key.contains(":capacity")),
                                                eq(780)); // default
                                                          // override

                // Total calls: 50 (custom) + 100 (default) = 150 calls for Venue1 capacity
                verify(valueOperations, times(150))
                                .set(argThat(key -> key.contains("venue:Venue1:zone:") && key.contains(":capacity")),
                                                anyInt());
        }
}
