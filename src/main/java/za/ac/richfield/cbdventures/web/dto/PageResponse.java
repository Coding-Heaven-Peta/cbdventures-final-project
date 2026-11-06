package za.ac.richfield.cbdventures.web.dto;

import java.util.List;
import java.util.Map;

/**
 * Envelope for list endpoints so the client can show "24 places" style copy and
 * know which filters were actually applied.
 */
public record PageResponse<T>(
        List<T> items,
        long total,
        int page,
        int size,
        int totalPages,
        String sort,
        Map<String, Object> appliedFilters,
        ZoneView zone
) {

    public static <T> PageResponse<T> of(List<T> items, int page, int size, String sort,
                                         Map<String, Object> appliedFilters, ZoneView zone) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil(items.size() / (double) size);
        return new PageResponse<>(items, items.size(), page, size, totalPages, sort, appliedFilters, zone);
    }
}
