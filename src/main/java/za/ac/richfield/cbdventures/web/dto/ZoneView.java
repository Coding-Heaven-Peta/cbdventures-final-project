package za.ac.richfield.cbdventures.web.dto;

/**
 * The geographic boundary the client draws and filters against.
 */
public record ZoneView(
        String name,
        String address,
        double anchorLatitude,
        double anchorLongitude,
        double primaryRadiusKm,
        double extendedRadiusKm,
        boolean extendedEnabled
) {
}
