package za.ac.richfield.cbdventures.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4 — unit tests for the geographic helpers that enforce the pilot zone.
 */
class GeoUtilsTest {

    private static final double RICHFIELD_LAT = -25.746350;
    private static final double RICHFIELD_LNG = 28.192550;

    @Test
    @DisplayName("Distance to the same point is zero")
    void samePointIsZero() {
        assertEquals(0.0, GeoUtils.distanceKm(RICHFIELD_LAT, RICHFIELD_LNG,
                RICHFIELD_LAT, RICHFIELD_LNG), 1e-9);
    }

    @Test
    @DisplayName("One degree of latitude is roughly 111 km")
    void latitudeDegree() {
        double km = GeoUtils.distanceKm(0, 0, 1, 0);
        assertEquals(111.19, km, 0.5);
    }

    @Test
    @DisplayName("A 500 m northern offset measures as 0.5 km")
    void halfKilometreOffset() {
        double metresPerDegreeLat = 111_320d;
        double north = RICHFIELD_LAT + (500 / metresPerDegreeLat);
        double km = GeoUtils.distanceKm(RICHFIELD_LAT, RICHFIELD_LNG, north, RICHFIELD_LNG);
        assertEquals(0.5, km, 0.01);
    }

    @Test
    @DisplayName("Distance is symmetric")
    void symmetry() {
        double forward = GeoUtils.distanceKm(-25.7463, 28.1925, -26.2041, 28.0473);
        double backward = GeoUtils.distanceKm(-26.2041, 28.0473, -25.7463, 28.1925);
        assertEquals(forward, backward, 1e-9);
    }

    @Test
    @DisplayName("Pretoria CBD to Johannesburg is about 55 km")
    void pretoriaToJohannesburg() {
        double km = GeoUtils.distanceKm(RICHFIELD_LAT, RICHFIELD_LNG, -26.2041, 28.0473);
        assertTrue(km > 50 && km < 60, "expected roughly 55 km but was " + km);
    }

    @Test
    @DisplayName("Rounding keeps two decimals")
    void rounding() {
        assertEquals(0.42, GeoUtils.round2(0.4218), 1e-9);
        assertEquals(1.0, GeoUtils.round2(0.999), 1e-9);
    }

    @Test
    @DisplayName("Walking time is at least one minute and grows with distance")
    void walkingMinutes() {
        assertEquals(1, GeoUtils.walkingMinutes(0.01));
        assertTrue(GeoUtils.walkingMinutes(1.0) > GeoUtils.walkingMinutes(0.5));
        assertEquals(13, GeoUtils.walkingMinutes(1.0));
    }
}
