package za.ac.richfield.cbdventures.web.dto;

/**
 * The smallest possible payload for plotting a listing on the zone map:
 * position, identity and just enough detail for the pin list.
 */
public record MapPin(
        Long id,
        String name,
        String categoryName,
        String iconKey,
        double latitude,
        double longitude,
        double rating,
        long reviewCount,
        Double distanceKm,
        Integer walkingMinutes,
        String priceLevel) {
}
