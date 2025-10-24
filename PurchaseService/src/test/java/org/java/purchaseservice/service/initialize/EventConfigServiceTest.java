package org.java.purchaseservice.service.initialize;

import org.java.purchaseservice.config.EventConfig;
import org.java.purchaseservice.service.redis.SeatOccupiedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for EventConfigService initialization process.
 * Verifies that event configurations are properly processed and events are
 * initialized.
 */
@ExtendWith(MockitoExtension.class)
class EventConfigServiceTest {

    @Mock
    private SeatOccupiedService seatOccupiedService;

    @Mock
    private EventConfig eventConfig;

    @Mock
    private ApplicationArguments applicationArguments;

    private EventConfigService eventConfigService;

    @BeforeEach
    void setUp() {
        eventConfigService = new EventConfigService(seatOccupiedService, eventConfig);
    }

    @Test
    void run_autoInitializeEnabled_initializesEnabledEvents() {
        // Given: Auto-initialize is enabled with multiple events
        when(eventConfig.isAutoInitialize()).thenReturn(true);

        EventConfig.Event enabledEvent1 = createEvent("EVENT001", "Concert A", "Venue1", true);
        EventConfig.Event enabledEvent2 = createEvent("EVENT002", "Concert B", "Venue2", true);
        EventConfig.Event disabledEvent = createEvent("EVENT003", "Concert C", "Venue1", false);

        List<EventConfig.Event> events = Arrays.asList(enabledEvent1, enabledEvent2, disabledEvent);
        when(eventConfig.getList()).thenReturn(events);

        // When: Run event initialization
        eventConfigService.initializeEvents();

        // Then: Only enabled events should be initialized
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT001", "Venue1");
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT002", "Venue2");
        verify(seatOccupiedService, never()).initializeAllZonesForEvent("EVENT003", "Venue1");

        // Should call initialization exactly 2 times (for 2 enabled events)
        verify(seatOccupiedService, times(2)).initializeAllZonesForEvent(anyString(), anyString());
    }

    @Test
    void run_autoInitializeDisabled_skipsAllInitialization() {
        // Given: Auto-initialize is disabled
        when(eventConfig.isAutoInitialize()).thenReturn(false);

        // When: Run event initialization
        eventConfigService.initializeEvents();

        // Then: No events should be initialized
        verify(seatOccupiedService, never()).initializeAllZonesForEvent(anyString(), anyString());
        verify(eventConfig, never()).getList(); // Should not even fetch the event list
    }

    @Test
    void run_emptyEventList_completesWithoutError() {
        // Given: Auto-initialize enabled but no events configured
        when(eventConfig.isAutoInitialize()).thenReturn(true);
        when(eventConfig.getList()).thenReturn(Collections.emptyList());

        // When: Run event initialization
        assertDoesNotThrow(() -> eventConfigService.initializeEvents());

        // Then: No initialization calls should be made
        verify(seatOccupiedService, never()).initializeAllZonesForEvent(anyString(), anyString());
    }

    @Test
    void run_nullEventList_completesWithoutError() {
        // Given: Auto-initialize enabled but event list is null
        when(eventConfig.isAutoInitialize()).thenReturn(true);
        when(eventConfig.getList()).thenReturn(null);

        // When: Run event initialization
        assertDoesNotThrow(() -> eventConfigService.initializeEvents());

        // Then: No initialization calls should be made
        verify(seatOccupiedService, never()).initializeAllZonesForEvent(anyString(), anyString());
    }

    @Test
    void run_singleEventInitializationFailure_continuesWithOtherEvents() {
        // Given: Multiple events, one will fail during initialization
        when(eventConfig.isAutoInitialize()).thenReturn(true);

        EventConfig.Event successEvent1 = createEvent("EVENT001", "Concert A", "Venue1", true);
        EventConfig.Event failingEvent = createEvent("EVENT002", "Concert B", "Venue2", true);
        EventConfig.Event successEvent2 = createEvent("EVENT003", "Concert C", "Venue3", true);

        List<EventConfig.Event> events = Arrays.asList(successEvent1, failingEvent, successEvent2);
        when(eventConfig.getList()).thenReturn(events);

        // Configure failing event
        doNothing().when(seatOccupiedService).initializeAllZonesForEvent("EVENT001", "Venue1");
        doThrow(new RuntimeException("Venue2 not found")).when(seatOccupiedService)
                .initializeAllZonesForEvent("EVENT002", "Venue2");
        doNothing().when(seatOccupiedService).initializeAllZonesForEvent("EVENT003", "Venue3");

        // When: Run event initialization (should not throw exception)
        assertDoesNotThrow(() -> eventConfigService.initializeEvents());

        // Then: All events should be attempted, including the failing one
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT001", "Venue1");
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT002", "Venue2");
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT003", "Venue3");

        // Total 3 attempts should be made
        verify(seatOccupiedService, times(3)).initializeAllZonesForEvent(anyString(), anyString());
    }

    @Test
    void run_mixedEnabledDisabledEvents_onlyProcessesEnabled() {
        // Given: Mix of enabled and disabled events
        when(eventConfig.isAutoInitialize()).thenReturn(true);

        EventConfig.Event event1 = createEvent("EVENT001", "Concert A", "Venue1", true); // enabled
        EventConfig.Event event2 = createEvent("EVENT002", "Concert B", "Venue2", false); // disabled
        EventConfig.Event event3 = createEvent("EVENT003", "Concert C", "Venue1", true); // enabled
        EventConfig.Event event4 = createEvent("EVENT004", "Concert D", "Venue3", false); // disabled
        EventConfig.Event event5 = createEvent("EVENT005", "Concert E", "Venue2", true); // enabled

        List<EventConfig.Event> events = Arrays.asList(event1, event2, event3, event4, event5);
        when(eventConfig.getList()).thenReturn(events);

        // When: Run event initialization
        eventConfigService.initializeEvents();

        // Then: Only enabled events (EVENT001, EVENT003, EVENT005) should be
        // initialized
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT001", "Venue1");
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT003", "Venue1");
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT005", "Venue2");

        // Disabled events should not be initialized
        verify(seatOccupiedService, never()).initializeAllZonesForEvent("EVENT002", "Venue2");
        verify(seatOccupiedService, never()).initializeAllZonesForEvent("EVENT004", "Venue3");

        // Total 3 enabled events should be processed
        verify(seatOccupiedService, times(3)).initializeAllZonesForEvent(anyString(), anyString());
    }

    @Test
    void run_allEventsDisabled_noInitializationPerformed() {
        // Given: All events are disabled
        when(eventConfig.isAutoInitialize()).thenReturn(true);

        EventConfig.Event disabledEvent1 = createEvent("EVENT001", "Concert A", "Venue1", false);
        EventConfig.Event disabledEvent2 = createEvent("EVENT002", "Concert B", "Venue2", false);
        EventConfig.Event disabledEvent3 = createEvent("EVENT003", "Concert C", "Venue3", false);

        List<EventConfig.Event> events = Arrays.asList(disabledEvent1, disabledEvent2, disabledEvent3);
        when(eventConfig.getList()).thenReturn(events);

        // When: Run event initialization
        eventConfigService.initializeEvents();

        // Then: No events should be initialized since all are disabled
        verify(seatOccupiedService, never()).initializeAllZonesForEvent(anyString(), anyString());
    }

    @Test
    void run_largeNumberOfEvents_processesAllSuccessfully() {
        // Given: Large number of enabled events (stress test)
        when(eventConfig.isAutoInitialize()).thenReturn(true);

        List<EventConfig.Event> manyEvents = Arrays.asList(
                createEvent("EVENT001", "Concert 1", "Venue1", true),
                createEvent("EVENT002", "Concert 2", "Venue1", true),
                createEvent("EVENT003", "Concert 3", "Venue2", true),
                createEvent("EVENT004", "Concert 4", "Venue2", true),
                createEvent("EVENT005", "Concert 5", "Venue3", true),
                createEvent("EVENT006", "Concert 6", "Venue3", true),
                createEvent("EVENT007", "Concert 7", "Venue1", true),
                createEvent("EVENT008", "Concert 8", "Venue2", true),
                createEvent("EVENT009", "Concert 9", "Venue3", true),
                createEvent("EVENT010", "Concert 10", "Venue1", true));

        when(eventConfig.getList()).thenReturn(manyEvents);

        // When: Run event initialization
        eventConfigService.initializeEvents();

        // Then: All 10 events should be initialized
        verify(seatOccupiedService, times(10)).initializeAllZonesForEvent(anyString(), anyString());

        // Verify specific calls for a few events
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT001", "Venue1");
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT005", "Venue3");
        verify(seatOccupiedService).initializeAllZonesForEvent("EVENT010", "Venue1");
    }

    /**
     * Helper method to create EventConfig.Event instances for testing
     */
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
