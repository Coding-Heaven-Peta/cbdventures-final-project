package za.ac.richfield.cbdventures.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import za.ac.richfield.cbdventures.domain.enums.PriceLevel;

import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record DecisionRequest(
            @Size(max = 255, message = "must be 255 characters or fewer")
            String note
    ) {
    }

    /** Recommendation preferences stored per visitor (FR-04). */
    public record PreferenceRequest(
            PriceLevel maxPriceLevel,
            Double maxDistanceKm,
            List<Long> preferredCategoryIds
    ) {
    }

    public record PreferenceView(
            PriceLevel maxPriceLevel,
            Double maxDistanceKm,
            List<Long> preferredCategoryIds,
            boolean personalised
    ) {
    }

    public record StatsView(
            long totalBusinesses,
            long activeBusinesses,
            long pendingBusinesses,
            long reviews,
            long users,
            long owners,
            long events,
            double averageRating,
            List<CategoryCount> byCategory
    ) {
    }

    public record CategoryCount(String name, String slug, long count) {
    }

    public record MessageView(@NotNull String message) {
    }
}
