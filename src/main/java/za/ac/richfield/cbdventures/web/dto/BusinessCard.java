package za.ac.richfield.cbdventures.web.dto;

import java.util.List;

/**
 * A business as it appears in a result list (search card / map pin).
 */
public record BusinessCard(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        String categorySlug,
        String iconKey,
        String priceLevel,
        String priceLabel,
        double rating,
        long reviewCount,
        Double distanceKm,
        Integer walkingMinutes,
        String address,
        String operatingHours,
        /** Today's trading window, e.g. "07:00 - 17:00", or "Closed today". */
        String todayHours,
        Boolean openNow,
        String thumbnail,
        List<String> amenities,
        String status
) {
}
