package za.ac.richfield.cbdventures.web.dto;

import java.util.List;

/**
 * Shape of a CBDVentures system backup (Phase 3 deliverable "System Backup").
 *
 * <p>The backup is a single JSON document containing reference data, accounts
 * (with their BCrypt hashes so passwords keep working after a restore) and all
 * transactional rows. It can be produced and consumed by the
 * {@code /api/admin/backup} endpoints.</p>
 */
public final class BackupDtos {

    private BackupDtos() {
    }

    public record CategoryRow(String name, String slug, String description, String iconKey, int sortOrder) {
    }

    public record AmenityRow(String name, String iconKey, String categorySlug) {
    }

    public record UserRow(String fullName, String email, String passwordHash, String role,
                          String createdAt) {
    }

    public record ImageRow(String url, String caption, boolean primary) {
    }

    public record BusinessRow(String name, String ownerEmail, String categorySlug, String description,
                              String address, String latitude, String longitude, String zoneRadius,
                              String priceLevel, String operatingHours, String openTime, String closeTime,
                              String phone, String whatsapp, String website, String status, String adminNote,
                              String createdAt, String updatedAt, List<String> amenityNames,
                              List<ImageRow> images, List<HoursRow> hours) {
    }

    /** One weekday of opening hours inside a backup. */
    public record HoursRow(int dayOfWeek, boolean closed, String openTime, String closeTime) {
    }

    public record EventRow(String businessName, String title, String description, String venue,
                           String eventDateTime, String ticketPrice) {
    }

    public record ReviewRow(String userEmail, String businessName, int rating, String comment,
                            boolean verified, String createdAt, String updatedAt) {
    }

    public record PreferenceRow(String userEmail, String maxPriceLevel, String maxDistanceKm,
                                List<String> preferredCategorySlugs) {
    }

    /** Counts shown in the backup summary. */
    public record BackupCounts(int categories, int amenities, int users, int businesses, int images,
                               int events, int reviews, int preferences, int hours) {
    }

    /** The backup document. */
    public record BackupFile(String format, int version, String generatedAt, String generatedBy,
                             BackupCounts counts, List<CategoryRow> categories, List<AmenityRow> amenities,
                             List<UserRow> users, List<BusinessRow> businesses, List<EventRow> events,
                             List<ReviewRow> reviews, List<PreferenceRow> preferences) {
    }

    /** Summary returned after a restore. */
    public record RestoreResult(String restoredAt, BackupCounts counts, String message) {
    }
}
