package za.ac.richfield.cbdventures.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.ac.richfield.cbdventures.config.ZoneProperties;
import za.ac.richfield.cbdventures.domain.enums.ZoneRadius;
import za.ac.richfield.cbdventures.error.ApiException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4 — unit tests for the geographic boundary rule (Phase 3, user-defined
 * integrity: "only listings whose Location falls within the 1 km radius … may
 * be approved and published").
 */
class ZoneServiceTest {

    private ZoneService service(boolean extendedEnabled) {
        ZoneProperties properties = new ZoneProperties(
                "Richfield Graduate Institute of Technology - Pretoria Campus",
                "291 Helen Joseph Street, Pretoria Central",
                -25.746350, 28.192550, 1.0, 5.0, extendedEnabled);
        return new ZoneService(properties);
    }

    @Test
    @DisplayName("The campus anchor itself sits in the primary zone")
    void anchorIsPrimary() {
        assertEquals(ZoneRadius.PRIMARY_1KM, service(false).classify(-25.746350, 28.192550));
    }

    @Test
    @DisplayName("A point 400 m away is still in the primary zone")
    void fourHundredMetresIsPrimary() {
        double lat = -25.746350 + (400 / 111_320d);
        assertEquals(ZoneRadius.PRIMARY_1KM, service(false).classify(lat, 28.192550));
    }

    @Test
    @DisplayName("A point 2 km away is classified as the extended zone")
    void twoKilometresIsExtended() {
        double lat = -25.746350 + (2000 / 111_320d);
        assertEquals(ZoneRadius.EXTENDED_5KM, service(false).classify(lat, 28.192550));
    }

    @Test
    @DisplayName("A point outside 5 km is rejected with a friendly message")
    void outsideFiveKilometresIsRejected() {
        ApiException error = assertThrows(ApiException.class,
                () -> service(false).classify(-26.2041, 28.0473));
        assertTrue(error.getMessage().contains("outside the 5 km CBD boundary"));
    }

    @Test
    @DisplayName("Extended-zone listings stay hidden until the zone is switched on")
    void extendedZoneVisibility() {
        assertTrue(service(false).isDiscoverable(ZoneRadius.PRIMARY_1KM));
        assertFalse(service(false).isDiscoverable(ZoneRadius.EXTENDED_5KM));
        assertTrue(service(true).isDiscoverable(ZoneRadius.EXTENDED_5KM));
    }

    @Test
    @DisplayName("The active browsing radius follows the configuration flag")
    void activeRadius() {
        assertEquals(1.0, service(false).activeRadiusKm(), 1e-9);
        assertEquals(5.0, service(true).activeRadiusKm(), 1e-9);
    }
}
