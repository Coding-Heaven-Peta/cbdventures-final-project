package za.ac.richfield.cbdventures.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Full business profile - everything shown on the profile screen described in
 * the Phase 2/3 UI requirements: header, name, category tag, hours, contact,
 * address, map pin, reviews and the "leave a review" action.
 */
public record BusinessDetail(
        Long id,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        String categorySlug,
        String iconKey,
        String priceLevel,
        String priceLabel,
        double rating,
        long reviewCount,
        Map<String, Long> ratingDistribution,
        Double distanceKm,
        Integer walkingMinutes,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String operatingHours,
        /** Today's trading window, e.g. "07:00 - 17:00", or "Closed today". */
        String todayHours,
        /** Full week of opening hours, Monday first. */
        List<HoursView> hours,
        Boolean openNow,
        String phone,
        String whatsapp,
        String website,
        String zoneRadius,
        String thumbnail,
        List<ImageView> images,
        List<CatalogDtos.AmenityView> amenities,
        List<EventDtos.EventView> events,
        List<ReviewDtos.ReviewView> reviews,
        String ownerName,
        String status,
        boolean mine,
        boolean canReview,
        ReviewDtos.ReviewView myReview,
        String directionsUrl,
        Instant createdAt,
        Instant updatedAt
) {

    public record ImageView(Long id, String url, String caption, boolean primary) {
    }

    /** One weekday of opening hours for display. */
    public record HoursView(
            int dayOfWeek,
            String dayName,
            String shortDayName,
            boolean closed,
            String openTime,
            String closeTime,
            boolean today
    ) {
    }
}
