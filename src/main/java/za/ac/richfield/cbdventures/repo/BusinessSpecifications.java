package za.ac.richfield.cbdventures.repo;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import za.ac.richfield.cbdventures.domain.Amenity;
import za.ac.richfield.cbdventures.domain.Business;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.domain.enums.PriceLevel;
import za.ac.richfield.cbdventures.domain.enums.ZoneRadius;

import java.util.ArrayList;
import java.util.List;

/**
 * Composable JPA criteria for the discovery endpoints. Only filtering that the
 * database can do efficiently lives here; rating, amenity, proximity and
 * "open now" filters are applied in memory because they depend on aggregates or
 * on the haversine distance.
 */
public final class BusinessSpecifications {

    private BusinessSpecifications() {
    }

    public static Specification<Business> status(BusinessStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Business> statusIn(BusinessStatus status) {
        return (root, query, cb) -> root.get("status").in(status);
    }

    public static Specification<Business> discoverable(boolean extendedEnabled) {
        return (root, query, cb) -> extendedEnabled
                ? root.get("zoneRadius").in(ZoneRadius.PRIMARY_1KM, ZoneRadius.EXTENDED_5KM)
                : cb.equal(root.get("zoneRadius"), ZoneRadius.PRIMARY_1KM);
    }

    public static Specification<Business> categorySlug(String slug) {
        if (slug == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("category").get("slug")), slug.toLowerCase());
    }

    public static Specification<Business> priceLevels(List<PriceLevel> levels) {
        if (levels == null || levels.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("priceLevel").in(levels);
    }

    /**
     * Free-text search across the business name, description, address, category
     * name and amenity names. Every token must match at least one field, so
     * "affordable coffee" narrows rather than widens the result set.
     */
    public static Specification<Business> keywords(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String[] tokens = keyword.toLowerCase().trim().split("\\s+");
        return (root, query, cb) -> {
            List<Predicate> perToken = new ArrayList<>();
            for (String token : tokens) {
                String like = "%" + token + "%";
                Predicate name = cb.like(cb.lower(root.get("name")), like);
                Predicate description = cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like);
                Predicate address = cb.like(cb.lower(root.get("address")), like);
                Predicate category = cb.like(cb.lower(root.get("category").get("name")), like);

                Subquery<Long> amenityMatch = query.subquery(Long.class);
                Root<Business> subRoot = amenityMatch.from(Business.class);
                Join<Business, Amenity> amenityJoin = subRoot.join("amenities");
                amenityMatch.select(subRoot.get("id")).where(
                        cb.equal(subRoot.get("id"), root.get("id")),
                        cb.like(cb.lower(amenityJoin.get("name")), like));

                perToken.add(cb.or(name, description, address, category, cb.exists(amenityMatch)));
            }
            return cb.and(perToken.toArray(new Predicate[0]));
        };
    }

    public static Specification<Business> ownerId(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }
}
