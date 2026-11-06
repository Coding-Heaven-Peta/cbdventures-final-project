package za.ac.richfield.cbdventures.web.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record ReviewView(
            Long id,
            String authorName,
            String initials,
            Integer rating,
            String comment,
            Instant createdAt,
            boolean verified,
            boolean mine
    ) {
    }

    public record ReviewRequest(
            @NotNull(message = "is required")
            @Min(value = 1, message = "must be between 1 and 5 stars")
            @Max(value = 5, message = "must be between 1 and 5 stars")
            Integer rating,

            @Size(max = 1000, message = "must be 1000 characters or fewer")
            String comment
    ) {
    }
}
