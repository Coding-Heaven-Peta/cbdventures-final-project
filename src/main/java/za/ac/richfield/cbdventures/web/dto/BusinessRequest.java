package za.ac.richfield.cbdventures.web.dto;

import jakarta.validation.constraints.*;
import za.ac.richfield.cbdventures.domain.enums.PriceLevel;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload used by a business owner to create or update a listing.
 * Validation mirrors the domain integrity rules in Phase 3 (Section 3.4).
 */
public record BusinessRequest(
        @NotBlank(message = "is required")
        @Size(max = 120, message = "must be 120 characters or fewer")
        String name,

        @Size(max = 3000, message = "must be 3000 characters or fewer")
        String description,

        @NotBlank(message = "is required")
        @Size(max = 255, message = "must be 255 characters or fewer")
        String address,

        @NotNull(message = "is required")
        @DecimalMin(value = "-90.0", message = "must be a valid latitude")
        @DecimalMax(value = "90.0", message = "must be a valid latitude")
        BigDecimal latitude,

        @NotNull(message = "is required")
        @DecimalMin(value = "-180.0", message = "must be a valid longitude")
        @DecimalMax(value = "180.0", message = "must be a valid longitude")
        BigDecimal longitude,

        @NotNull(message = "is required")
        PriceLevel priceLevel,

        @NotNull(message = "is required")
        Long categoryId,

        String operatingHours,
        String phone,
        String whatsapp,
        String website,

        /** Legacy single-window hours (optional, but both or neither). */
        String openTime,
        String closeTime,

        /**
         * Per-day opening hours. When supplied this is authoritative and the
         * legacy openTime/closeTime fields are ignored. Days omitted from the
         * list are treated as "no information" for that day.
         */
        @Size(max = 7, message = "cannot contain more than 7 days")
        List<HoursRequest> hours,

        List<Long> amenityIds,
        List<String> imageUrls,

        /** Existing reviews are unaffected; set false to temporarily hide a listing. */
        Boolean reactivate
) {

    /**
     * One weekday of opening hours.
     *
     * @param dayOfWeek ISO weekday, 1 = Monday ... 7 = Sunday
     * @param closed    true for a day the venue does not trade
     */
    public record HoursRequest(
            @NotNull(message = "is required")
            @Min(value = 1, message = "must be 1 (Monday) to 7 (Sunday)")
            @Max(value = 7, message = "must be 1 (Monday) to 7 (Sunday)")
            Integer dayOfWeek,

            Boolean closed,
            String openTime,
            String closeTime
    ) {
    }
}
