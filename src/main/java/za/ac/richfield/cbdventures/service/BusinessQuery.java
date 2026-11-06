package za.ac.richfield.cbdventures.service;

import za.ac.richfield.cbdventures.domain.enums.PriceLevel;

import java.util.List;

/**
 * Search and filter criteria. Mirrors the FR-01 requirement: filter by
 * category, price range, proximity (1 km) and ratings, then sort by distance,
 * price or reviews.
 */
public record BusinessQuery(
        String q,
        String category,
        List<Long> priceLevels,
        Double minRating,
        Double maxDistanceKm,
        List<Long> amenityIds,
        Boolean openNow,
        String sort,
        int page,
        int size,
        Double latitude,
        Double longitude
) {

    public static BusinessQuery defaults() {
        return new BusinessQuery(null, null, null, null, null, null, null,
                "recommended", 0, 60, null, null);
    }

    public BusinessQuery normalised(int maxPageSize) {
        int safeSize = size <= 0 ? 24 : Math.min(size, maxPageSize);
        int safePage = Math.max(page, 0);
        String safeSort = (sort == null || sort.isBlank()) ? "recommended" : sort.trim().toLowerCase();
        return new BusinessQuery(
                q == null || q.isBlank() ? null : q.trim(),
                category == null || category.isBlank() ? null : category.trim(),
                priceLevels == null || priceLevels.isEmpty() ? null : priceLevels,
                minRating,
                maxDistanceKm,
                amenityIds == null || amenityIds.isEmpty() ? null : amenityIds,
                openNow,
                safeSort,
                safePage,
                safeSize,
                latitude,
                longitude);
    }

    public List<PriceLevel> priceLevelEnums() {
        if (priceLevels == null) {
            return List.of();
        }
        return priceLevels.stream()
                .map(index -> switch (index.intValue()) {
                    case 1 -> PriceLevel.R;
                    case 2 -> PriceLevel.RR;
                    case 3 -> PriceLevel.RRR;
                    default -> null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
