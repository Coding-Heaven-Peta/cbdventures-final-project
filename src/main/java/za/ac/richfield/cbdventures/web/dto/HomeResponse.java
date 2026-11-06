package za.ac.richfield.cbdventures.web.dto;

import java.util.List;
import java.util.Map;

/**
 * Everything the home screen needs in a single round trip: five category tiles,
 * curated suggestions, upcoming events and pilot-zone statistics.
 */
public record HomeResponse(
        ZoneView zone,
        List<CatalogDtos.CategoryView> categories,
        List<BusinessCard> featured,
        List<BusinessCard> recommended,
        List<EventDtos.EventView> upcomingEvents,
        Map<String, Object> stats,
        Map<String, Object> personalisation
) {
}
