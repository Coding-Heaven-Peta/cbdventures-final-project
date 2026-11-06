package za.ac.richfield.cbdventures.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import za.ac.richfield.cbdventures.domain.Amenity;
import za.ac.richfield.cbdventures.domain.Business;
import za.ac.richfield.cbdventures.domain.BusinessHours;
import za.ac.richfield.cbdventures.domain.BusinessImage;
import za.ac.richfield.cbdventures.domain.Category;
import za.ac.richfield.cbdventures.domain.EventListing;
import za.ac.richfield.cbdventures.domain.Review;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.domain.UserPreference;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.domain.enums.PriceLevel;
import za.ac.richfield.cbdventures.domain.enums.Role;
import za.ac.richfield.cbdventures.domain.enums.ZoneRadius;
import za.ac.richfield.cbdventures.error.ApiException;
import za.ac.richfield.cbdventures.repo.AmenityRepository;
import za.ac.richfield.cbdventures.repo.BusinessRepository;
import za.ac.richfield.cbdventures.repo.CategoryRepository;
import za.ac.richfield.cbdventures.repo.EventRepository;
import za.ac.richfield.cbdventures.repo.ReviewRepository;
import za.ac.richfield.cbdventures.repo.UserPreferenceRepository;
import za.ac.richfield.cbdventures.repo.UserRepository;
import za.ac.richfield.cbdventures.web.dto.BackupDtos;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * System backup and restore (Phase 3 deliverable "System Backup").
 *
 * <p>Two complementary strategies are supported and documented in
 * {@code docs/backup-and-restore.md}:</p>
 * <ol>
 *   <li><b>Logical backup</b> - this service exports every table to one JSON
 *       document and can restore it again. Works identically on H2 and MySQL and
 *       is what the administrator screen uses.</li>
 *   <li><b>Physical backup</b> - copy the H2 file or run {@code mysqldump}. See
 *       the runbook for commands.</li>
 * </ol>
 *
 * <p>Password hashes are exported so an account can still sign in after a
 * restore. The document is therefore administrator-only and must be treated as
 * sensitive (POPIA).</p>
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final String FORMAT = "cbdventures-backup";
    private static final int VERSION = 1;
    private static final ZoneId ZONE = ZoneId.of("Africa/Johannesburg");
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ENGLISH);

    private final CategoryRepository categoryRepository;
    private final AmenityRepository amenityRepository;
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final EventRepository eventRepository;
    private final ReviewRepository reviewRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public BackupService(CategoryRepository categoryRepository,
                         AmenityRepository amenityRepository,
                         UserRepository userRepository,
                         BusinessRepository businessRepository,
                         EventRepository eventRepository,
                         ReviewRepository reviewRepository,
                         UserPreferenceRepository preferenceRepository,
                         ObjectMapper objectMapper) {
        this.categoryRepository = categoryRepository;
        this.amenityRepository = amenityRepository;
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.eventRepository = eventRepository;
        this.reviewRepository = reviewRepository;
        this.preferenceRepository = preferenceRepository;
        this.objectMapper = objectMapper;
    }

    /* ------------------------------------------------------------- export */

    @Transactional(readOnly = true)
    public BackupDtos.BackupFile export(User admin) {
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Only administrators can download a system backup.");
        }

        List<Category> categories = categoryRepository.findAllByOrderBySortOrderAsc();
        List<Amenity> amenities = amenityRepository.findAllByOrderByNameAsc();
        List<User> users = userRepository.findAll();
        List<Business> businesses = businessRepository.findAll();
        List<EventListing> events = eventRepository.findAll();
        List<Review> reviews = reviewRepository.findAll();
        List<UserPreference> preferences = preferenceRepository.findAll();

        List<BackupDtos.CategoryRow> categoryRows = categories.stream()
                .map(c -> new BackupDtos.CategoryRow(c.getName(), c.getSlug(), c.getDescription(),
                        c.getIconKey(), c.getSortOrder() == null ? 0 : c.getSortOrder()))
                .toList();

        List<BackupDtos.AmenityRow> amenityRows = amenities.stream()
                .map(a -> new BackupDtos.AmenityRow(a.getName(), a.getIconKey(),
                        a.getCategory() == null ? null : a.getCategory().getSlug()))
                .toList();

        List<BackupDtos.UserRow> userRows = users.stream()
                .map(u -> new BackupDtos.UserRow(u.getFullName(), u.getEmail(), u.getPasswordHash(),
                        u.getRole().name(), instant(u.getCreatedAt())))
                .toList();

        List<BackupDtos.BusinessRow> businessRows = businesses.stream()
                .map(b -> new BackupDtos.BusinessRow(
                        b.getName(),
                        b.getOwner().getEmail(),
                        b.getCategory().getSlug(),
                        b.getDescription(),
                        b.getAddress(),
                        b.getLatitude().toPlainString(),
                        b.getLongitude().toPlainString(),
                        b.getZoneRadius().name(),
                        b.getPriceLevel().name(),
                        b.getOperatingHours(),
                        b.getOpenTime() == null ? null : b.getOpenTime().toString(),
                        b.getCloseTime() == null ? null : b.getCloseTime().toString(),
                        b.getPhone(),
                        b.getWhatsapp(),
                        b.getWebsite(),
                        b.getStatus().name(),
                        b.getAdminNote(),
                        instant(b.getCreatedAt()),
                        instant(b.getUpdatedAt()),
                        b.getAmenities().stream().map(Amenity::getName).sorted().toList(),
                        b.getImages().stream()
                                .map(i -> new BackupDtos.ImageRow(i.getImageUrl(), i.getCaption(), i.isPrimary()))
                                .toList(),
                        b.getHours().stream()
                                .map(h -> new BackupDtos.HoursRow(h.getDayOfWeek().getValue(), h.isClosed(),
                                        h.getOpenTime() == null ? null : h.getOpenTime().toString(),
                                        h.getCloseTime() == null ? null : h.getCloseTime().toString()))
                                .toList()))
                .toList();

        List<BackupDtos.EventRow> eventRows = events.stream()
                .map(e -> new BackupDtos.EventRow(e.getBusiness().getName(), e.getTitle(), e.getDescription(),
                        e.getVenue(),
                        e.getEventDateTime() == null ? null : e.getEventDateTime().toString(),
                        e.getTicketPrice() == null ? "0" : e.getTicketPrice().toPlainString()))
                .toList();

        List<BackupDtos.ReviewRow> reviewRows = reviews.stream()
                .map(r -> new BackupDtos.ReviewRow(r.getUser().getEmail(), r.getBusiness().getName(),
                        r.getRating(), r.getComment(), r.isVerified(),
                        instant(r.getCreatedAt()), instant(r.getUpdatedAt())))
                .toList();

        List<BackupDtos.PreferenceRow> preferenceRows = preferences.stream()
                .map(p -> new BackupDtos.PreferenceRow(p.getUser().getEmail(),
                        p.getMaxPriceLevel() == null ? null : p.getMaxPriceLevel().name(),
                        p.getMaxDistanceKm() == null ? null : String.valueOf(p.getMaxDistanceKm()),
                        p.getPreferredCategories().stream().map(Category::getSlug).sorted().toList()))
                .toList();

        BackupDtos.BackupCounts counts = new BackupDtos.BackupCounts(
                categoryRows.size(), amenityRows.size(), userRows.size(), businessRows.size(),
                businessRows.stream().mapToInt(b -> b.images().size()).sum(),
                eventRows.size(), reviewRows.size(), preferenceRows.size(),
                businessRows.stream().mapToInt(b -> b.hours().size()).sum());

        log.info("System backup generated by {}: {}", admin.getEmail(), counts);
        return new BackupDtos.BackupFile(FORMAT, VERSION, now(), admin.getEmail(), counts,
                categoryRows, amenityRows, userRows, businessRows, eventRows, reviewRows, preferenceRows);
    }

    /** File name for the download, e.g. {@code cbdventures-backup-20261027-184500.json}. */
    public String suggestedFileName() {
        return "cbdventures-backup-" + LocalDateTime.now(ZONE).format(STAMP) + ".json";
    }

    /* ------------------------------------------------------------ restore */

    /**
     * Replaces the current contents of the database with the supplied backup.
     * Runs in one transaction: if any row fails to load, nothing is changed.
     */
    @Transactional
    public BackupDtos.RestoreResult restore(User admin, String json) {
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Only administrators can restore a system backup.");
        }
        if (json == null || json.isBlank()) {
            throw ApiException.badRequest("The backup file is empty.");
        }

        BackupDtos.BackupFile backup;
        try {
            backup = objectMapper.readValue(json, BackupDtos.BackupFile.class);
        } catch (RuntimeException exception) {
            throw ApiException.badRequest(
                    "That file is not a valid CBDVentures backup. Upload the .json file produced by "
                            + "\"Download backup\".");
        }
        if (backup == null || !FORMAT.equals(backup.format())) {
            throw ApiException.badRequest("That file is not a CBDVentures backup (wrong format marker).");
        }
        if (backup.version() > VERSION) {
            throw ApiException.badRequest("This backup was created by a newer version of CBDVentures.");
        }
        if (backup.categories() == null || backup.categories().isEmpty()) {
            throw ApiException.badRequest("The backup contains no categories, so it cannot be restored.");
        }

        wipe();

        // 1. Reference data -------------------------------------------------
        Map<String, Category> categories = new HashMap<>();
        for (BackupDtos.CategoryRow row : backup.categories()) {
            Category category = categoryRepository.save(
                    new Category(row.name(), row.slug(), row.description(), row.iconKey(), row.sortOrder()));
            categories.put(row.slug(), category);
        }

        Map<String, Amenity> amenities = new HashMap<>();
        if (backup.amenities() != null) {
            for (BackupDtos.AmenityRow row : backup.amenities()) {
                Category category = row.categorySlug() == null ? null : categories.get(row.categorySlug());
                Amenity amenity = amenityRepository.save(new Amenity(category, row.name(), row.iconKey()));
                amenities.put(row.name(), amenity);
            }
        }

        // 2. Accounts (hashes preserved so passwords keep working) ----------
        Map<String, User> users = new HashMap<>();
        if (backup.users() != null) {
            for (BackupDtos.UserRow row : backup.users()) {
                User user = userRepository.save(new User(row.fullName(), row.email(),
                        row.passwordHash(), Role.valueOf(row.role())));
                User saved = user;
                saved.setCreatedAt(parseInstant(row.createdAt()));
                users.put(row.email().toLowerCase(Locale.ROOT), userRepository.save(saved));
            }
        }

        // 3. Listings, images and amenities --------------------------------
        Map<String, Business> businesses = new HashMap<>();
        if (backup.businesses() != null) {
            for (BackupDtos.BusinessRow row : backup.businesses()) {
                User owner = users.get(row.ownerEmail() == null ? "" : row.ownerEmail().toLowerCase(Locale.ROOT));
                Category category = categories.get(row.categorySlug());
                if (owner == null || category == null) {
                    throw ApiException.badRequest("Backup references an unknown owner or category for \""
                            + row.name() + "\".");
                }
                Business business = new Business();
                business.setOwner(owner);
                business.setCategory(category);
                business.setName(row.name());
                business.setDescription(row.description());
                business.setAddress(row.address());
                business.setLatitude(new BigDecimal(row.latitude()));
                business.setLongitude(new BigDecimal(row.longitude()));
                business.setZoneRadius(row.zoneRadius() == null ? ZoneRadius.PRIMARY_1KM
                        : ZoneRadius.valueOf(row.zoneRadius()));
                business.setPriceLevel(row.priceLevel() == null ? PriceLevel.R : PriceLevel.valueOf(row.priceLevel()));
                business.setOperatingHours(row.operatingHours());
                business.setOpenTime(row.openTime() == null ? null : LocalTime.parse(row.openTime()));
                business.setCloseTime(row.closeTime() == null ? null : LocalTime.parse(row.closeTime()));
                if (row.hours() != null && !row.hours().isEmpty()) {
                    for (BackupDtos.HoursRow h : row.hours()) {
                        business.addHours(h.closed()
                                ? BusinessHours.closed(DayOfWeek.of(h.dayOfWeek()))
                                : BusinessHours.open(DayOfWeek.of(h.dayOfWeek()),
                                        LocalTime.parse(h.openTime()), LocalTime.parse(h.closeTime())));
                    }
                }
                business.setPhone(row.phone());
                business.setWhatsapp(row.whatsapp());
                business.setWebsite(row.website());
                business.setStatus(row.status() == null ? BusinessStatus.PENDING
                        : BusinessStatus.valueOf(row.status()));
                business.setAdminNote(row.adminNote());
                business.setCreatedAt(parseInstant(row.createdAt()));
                business.setUpdatedAt(parseInstant(row.updatedAt()));

                Set<Amenity> linked = new LinkedHashSet<>();
                if (row.amenityNames() != null) {
                    for (String name : row.amenityNames()) {
                        Amenity amenity = amenities.get(name);
                        if (amenity != null) {
                            linked.add(amenity);
                        }
                    }
                }
                business.setAmenities(linked);

                if (row.images() != null) {
                    for (BackupDtos.ImageRow image : row.images()) {
                        business.addImage(new BusinessImage(image.url(), image.caption(), image.primary()));
                    }
                }
                businesses.put(business.getName(), businessRepository.save(business));
            }
        }

        // 4. Events ---------------------------------------------------------
        int eventCount = 0;
        if (backup.events() != null) {
            for (BackupDtos.EventRow row : backup.events()) {
                Business business = businesses.get(row.businessName());
                if (business == null) {
                    continue;
                }
                EventListing event = new EventListing();
                event.setBusiness(business);
                event.setTitle(row.title());
                event.setDescription(row.description());
                event.setVenue(row.venue());
                event.setEventDateTime(row.eventDateTime() == null ? null
                        : LocalDateTime.parse(row.eventDateTime()));
                event.setTicketPrice(row.ticketPrice() == null || row.ticketPrice().isBlank()
                        ? BigDecimal.ZERO : new BigDecimal(row.ticketPrice()));
                eventRepository.save(event);
                eventCount++;
            }
        }

        // 5. Reviews --------------------------------------------------------
        int reviewCount = 0;
        if (backup.reviews() != null) {
            for (BackupDtos.ReviewRow row : backup.reviews()) {
                User user = users.get(row.userEmail() == null ? "" : row.userEmail().toLowerCase(Locale.ROOT));
                Business business = businesses.get(row.businessName());
                if (user == null || business == null) {
                    continue;
                }
                Review review = new Review();
                review.setUser(user);
                review.setBusiness(business);
                review.setRating(row.rating());
                review.setComment(row.comment());
                review.setVerified(row.verified());
                review.setCreatedAt(parseInstant(row.createdAt()));
                review.setUpdatedAt(parseInstant(row.updatedAt()));
                reviewRepository.save(review);
                reviewCount++;
            }
        }

        // 6. Preferences ----------------------------------------------------
        int preferenceCount = 0;
        if (backup.preferences() != null) {
            for (BackupDtos.PreferenceRow row : backup.preferences()) {
                User user = users.get(row.userEmail() == null ? "" : row.userEmail().toLowerCase(Locale.ROOT));
                if (user == null) {
                    continue;
                }
                UserPreference preference = new UserPreference(user);
                preference.setMaxPriceLevel(row.maxPriceLevel() == null ? null
                        : PriceLevel.valueOf(row.maxPriceLevel()));
                preference.setMaxDistanceKm(row.maxDistanceKm() == null ? null
                        : Double.valueOf(row.maxDistanceKm()));
                Set<Category> preferred = new LinkedHashSet<>();
                if (row.preferredCategorySlugs() != null) {
                    for (String slug : row.preferredCategorySlugs()) {
                        Category category = categories.get(slug);
                        if (category != null) {
                            preferred.add(category);
                        }
                    }
                }
                preference.setPreferredCategories(preferred);
                preferenceRepository.save(preference);
                preferenceCount++;
            }
        }

        BackupDtos.BackupCounts counts = new BackupDtos.BackupCounts(categories.size(), amenities.size(),
                users.size(), businesses.size(),
                businesses.values().stream().mapToInt(b -> b.getImages().size()).sum(),
                eventCount, reviewCount, preferenceCount,
                businesses.values().stream().mapToInt(b -> b.getHours().size()).sum());
        log.warn("System restore completed by {}: {}", admin.getEmail(), counts);
        return new BackupDtos.RestoreResult(now(), counts,
                "Backup restored: " + counts.businesses() + " listings, " + counts.reviews()
                        + " reviews and " + counts.users() + " accounts.");
    }

    /**
     * Tables in the order they may be emptied: children before parents, so the
     * foreign keys ({@code RESTRICT} on app_user/category) never fire.
     */
    private static final List<String> DELETE_ORDER = List.of(
            "review",
            "event",
            "business_image",
            "business_hours",
            "user_activity",
            "business_amenity",
            "preference_category",
            "user_preference",
            "business",
            "app_user",
            "amenity",
            "category");

    /**
     * Empties every table in foreign-key-safe order using set-based deletes
     * (no ORM cascade), which behaves identically on H2 and MySQL and leaves
     * the persistence context clean for the re-import.
     */
    private void wipe() {
        entityManager.flush();
        for (String table : DELETE_ORDER) {
            entityManager.createNativeQuery("delete from " + table).executeUpdate();
        }
        entityManager.flush();
        entityManager.clear();
    }

    /* ------------------------------------------------------------ helpers */

    private String instant(Instant value) {
        return value == null ? null : value.toString();
    }

    private Instant parseInstant(String value) {
        return value == null || value.isBlank() ? Instant.now() : Instant.parse(value);
    }

    private String now() {
        return Instant.now().atZone(ZONE).toString();
    }

    /** Exposed for the restore test: a minimal valid backup document. */
    public static int currentVersion() {
        return VERSION;
    }
}
