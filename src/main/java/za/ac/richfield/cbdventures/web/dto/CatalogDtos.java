package za.ac.richfield.cbdventures.web.dto;

/**
 * Catalogue payloads: categories and amenities (read-only reference data).
 */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CategoryView(
            Long id,
            String name,
            String slug,
            String description,
            String iconKey,
            long businessCount
    ) {
    }

    public record AmenityView(
            Long id,
            String name,
            String iconKey,
            String categoryName
    ) {
    }
}
