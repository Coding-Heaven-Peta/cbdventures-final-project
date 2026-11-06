package za.ac.richfield.cbdventures.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class EventDtos {

    private EventDtos() {
    }

    public record EventView(
            Long id,
            String title,
            String description,
            String venue,
            LocalDateTime eventDateTime,
            BigDecimal ticketPrice,
            boolean free,
            Long businessId,
            String businessName,
            String businessAddress,
            String categoryName,
            Double distanceKm,
            String thumbnail
    ) {
    }

    public record EventRequest(
            @NotBlank(message = "is required")
            @Size(max = 100, message = "must be 100 characters or fewer")
            String title,

            @Size(max = 2000, message = "must be 2000 characters or fewer")
            String description,

            @Size(max = 100, message = "must be 100 characters or fewer")
            String venue,

            @NotNull(message = "is required")
            @Future(message = "must be a future date and time")
            LocalDateTime eventDateTime,

            @NotNull(message = "is required")
            @DecimalMin(value = "0.00", message = "cannot be negative")
            BigDecimal ticketPrice
    ) {
    }
}
