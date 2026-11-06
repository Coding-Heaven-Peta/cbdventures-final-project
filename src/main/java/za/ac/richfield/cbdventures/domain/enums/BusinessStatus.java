package za.ac.richfield.cbdventures.domain.enums;

/**
 * Listing lifecycle. Phase 3 business rule: "new Business listings created by an
 * Owner remain in a pending state until minimum required fields are complete,
 * preventing sparse or low-quality listings from appearing in search results."
 * Only ACTIVE listings are returned by public discovery endpoints.
 */
public enum BusinessStatus {
    PENDING,
    ACTIVE,
    REJECTED,
    INACTIVE
}
