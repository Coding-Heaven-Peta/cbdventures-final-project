package za.ac.richfield.cbdventures.util;

/**
 * Great-circle distance helpers. The zone boundary rule ("within 1 km of
 * Richfield") is enforced with the Haversine formula so that the same validation
 * applies to both H2 (demo) and MySQL (production) without relying on spatial
 * database extensions.
 */
public final class GeoUtils {

    /** Mean earth radius in kilometres. */
    public static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoUtils() {
    }

    /**
     * @return distance in kilometres between two WGS84 points.
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /** Rounds to two decimals for display-friendly distances (e.g. 0.42 km). */
    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Approximate walking time in minutes for a straight-line distance,
     * assuming an average walking speed of 4.5 km/h.
     */
    public static int walkingMinutes(double distanceKm) {
        return (int) Math.max(1, Math.round((distanceKm / 4.5) * 60));
    }
}
