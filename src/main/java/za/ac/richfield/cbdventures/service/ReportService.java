package za.ac.richfield.cbdventures.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.richfield.cbdventures.domain.Business;
import za.ac.richfield.cbdventures.domain.Category;
import za.ac.richfield.cbdventures.domain.EventListing;
import za.ac.richfield.cbdventures.domain.Review;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.error.ApiException;
import za.ac.richfield.cbdventures.repo.BusinessRepository;
import za.ac.richfield.cbdventures.repo.CategoryRepository;
import za.ac.richfield.cbdventures.repo.EventRepository;
import za.ac.richfield.cbdventures.repo.ReviewRepository;
import za.ac.richfield.cbdventures.repo.UserRepository;
import za.ac.richfield.cbdventures.util.GeoUtils;
import za.ac.richfield.cbdventures.web.dto.ReportDtos;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Management reports (Phase 3 deliverable "Reports").
 *
 * <p>Five reports cover the questions the pilot needs to answer: what is listed,
 * what awaits verification, how reviews are trending, which owners are active and
 * what the events calendar looks like. Each report is produced as a generic
 * table ({@link ReportDtos.ReportView}) that is rendered in the browser and can be
 * downloaded as CSV.</p>
 */
@Service
public class ReportService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Johannesburg");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);

    private final BusinessRepository businessRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final ZoneService zoneService;

    public ReportService(BusinessRepository businessRepository,
                         ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         CategoryRepository categoryRepository,
                         EventRepository eventRepository,
                         ZoneService zoneService) {
        this.businessRepository = businessRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.zoneService = zoneService;
    }

    /** Catalogue entries for the report picker. */
    public List<ReportDtos.ReportInfo> catalogue() {
        return List.of(
                new ReportDtos.ReportInfo("listings-by-category", "Listings by category",
                        "Every category with its live, pending and rejected listing counts and average rating."),
                new ReportDtos.ReportInfo("verification-audit", "Verification audit",
                        "Submission status of every listing with the administrator decision note."),
                new ReportDtos.ReportInfo("review-activity", "Review activity",
                        "Review volume and the 1-5 star spread for each listed business."),
                new ReportDtos.ReportInfo("owner-performance", "Owner performance",
                        "Listings, live listings, events and reviews received per business owner."),
                new ReportDtos.ReportInfo("events-schedule", "Events schedule",
                        "Chronological calendar of every event tied to a listed venue."));
    }

    @Transactional(readOnly = true)
    public ReportDtos.ReportView run(String key) {
        return switch (key == null ? "" : key) {
            case "listings-by-category" -> listingsByCategory();
            case "verification-audit" -> verificationAudit();
            case "review-activity" -> reviewActivity();
            case "owner-performance" -> ownerPerformance();
            case "events-schedule" -> eventsSchedule();
            default -> throw ApiException.notFound("That report does not exist.");
        };
    }

    /** Report 1 - listings grouped by category. */
    private ReportDtos.ReportView listingsByCategory() {
        List<Category> categories = categoryRepository.findAllByOrderBySortOrderAsc();
        List<Business> businesses = businessRepository.findAll();

        Map<Long, List<Business>> byCategory = new LinkedHashMap<>();
        categories.forEach(category -> byCategory.put(category.getId(), new ArrayList<>()));
        for (Business business : businesses) {
            byCategory.computeIfAbsent(business.getCategory().getId(), id -> new ArrayList<>()).add(business);
        }

        List<List<String>> rows = new ArrayList<>();
        long liveTotal = 0;
        long pendingTotal = 0;
        long rejectedTotal = 0;
        for (Category category : categories) {
            List<Business> listed = byCategory.getOrDefault(category.getId(), List.of());
            long live = count(listed, BusinessStatus.ACTIVE);
            long pending = count(listed, BusinessStatus.PENDING);
            long rejected = count(listed, BusinessStatus.REJECTED);
            liveTotal += live;
            pendingTotal += pending;
            rejectedTotal += rejected;
            rows.add(List.of(
                    category.getName(),
                    String.valueOf(live),
                    String.valueOf(pending),
                    String.valueOf(rejected),
                    String.valueOf(listed.size()),
                    decimal(averageRating(listed)),
                    String.valueOf(reviewCount(listed))));
        }
        rows.add(List.of("TOTAL",
                String.valueOf(liveTotal),
                String.valueOf(pendingTotal),
                String.valueOf(rejectedTotal),
                String.valueOf(businesses.size()),
                decimal(averageRating(businesses)),
                String.valueOf(reviewCount(businesses))));

        return new ReportDtos.ReportView(
                "listings-by-category",
                "Listings by category",
                "Verified, pending and rejected listings in the 1 km pilot zone, per category.",
                now(),
                List.of("Category", "Live", "Pending", "Rejected", "Total", "Average rating", "Reviews"),
                rows,
                List.of(
                        new ReportDtos.Metric("Live listings", String.valueOf(liveTotal)),
                        new ReportDtos.Metric("Awaiting review", String.valueOf(pendingTotal)),
                        new ReportDtos.Metric("Categories", String.valueOf(categories.size())),
                        new ReportDtos.Metric("Average rating", decimal(averageRating(businesses)))));
    }

    /** Report 2 - verification audit trail. Two pending, three rejected, etc. */
    private ReportDtos.ReportView verificationAudit() {
        List<Business> businesses = new ArrayList<>(businessRepository.findAll());
        businesses.sort(Comparator
                .comparing((Business b) -> b.getStatus() != BusinessStatus.PENDING)
                .thenComparing(Business::getCreatedAt, Comparator.reverseOrder()));

        List<List<String>> rows = new ArrayList<>();
        for (Business business : businesses) {
            rows.add(List.of(
                    business.getName(),
                    business.getCategory().getName(),
                    business.getOwner().getFullName(),
                    humanStatus(business.getStatus()),
                    blankToDash(business.getAdminNote()),
                    localDate(business.getCreatedAt()),
                    metres(distanceFromAnchor(business))));
        }
        long pending = count(businesses, BusinessStatus.PENDING);
        long live = count(businesses, BusinessStatus.ACTIVE);
        long rejected = count(businesses, BusinessStatus.REJECTED);
        long missingNote = businesses.stream()
                .filter(b -> b.getAdminNote() == null || b.getAdminNote().isBlank())
                .count();

        return new ReportDtos.ReportView(
                "verification-audit",
                "Verification audit",
                "Audit trail of every submission: who owns it, its status and the administrator note.",
                now(),
                List.of("Business", "Category", "Owner", "Status", "Administrator note", "Submitted", "Distance"),
                rows,
                List.of(
                        new ReportDtos.Metric("Awaiting review", String.valueOf(pending)),
                        new ReportDtos.Metric("Live", String.valueOf(live)),
                        new ReportDtos.Metric("Rejected", String.valueOf(rejected)),
                        new ReportDtos.Metric("Without a note", String.valueOf(missingNote))));
    }

    /** Report 3 - review activity and star distribution. */
    private ReportDtos.ReportView reviewActivity() {
        List<Business> businesses = businessRepository.findAll().stream()
                .filter(b -> b.getStatus() == BusinessStatus.ACTIVE)
                .sorted(Comparator.comparing(Business::getName))
                .toList();

        Map<Long, List<Review>> reviewsByBusiness = new LinkedHashMap<>();
        for (Review review : reviewRepository.findAll()) {
            reviewsByBusiness.computeIfAbsent(review.getBusiness().getId(), id -> new ArrayList<>()).add(review);
        }

        List<List<String>> rows = new ArrayList<>();
        long total = 0;
        long fiveStar = 0;
        for (Business business : businesses) {
            List<Review> reviews = reviewsByBusiness.getOrDefault(business.getId(), List.of());
            total += reviews.size();
            fiveStar += reviews.stream().filter(r -> r.getRating() != null && r.getRating() == 5).count();
            rows.add(List.of(
                    business.getName(),
                    business.getCategory().getName(),
                    String.valueOf(reviews.size()),
                    decimal(averageReviewRating(reviews)),
                    String.valueOf(starCount(reviews, 5)),
                    String.valueOf(starCount(reviews, 4)),
                    String.valueOf(starCount(reviews, 3)),
                    String.valueOf(starCount(reviews, 2)),
                    String.valueOf(starCount(reviews, 1)),
                    reviews.isEmpty() ? "—" : localDate(reviews.stream()
                            .map(Review::getCreatedAt)
                            .max(Comparator.naturalOrder()).orElse(null))));
        }

        return new ReportDtos.ReportView(
                "review-activity",
                "Review activity",
                "How many reviews each live listing has, the average score and the 1-5 star spread.",
                now(),
                List.of("Business", "Category", "Reviews", "Average", "5 star", "4 star", "3 star",
                        "2 star", "1 star", "Latest review"),
                rows,
                List.of(
                        new ReportDtos.Metric("Reviews", String.valueOf(total)),
                        new ReportDtos.Metric("Listings reviewed", String.valueOf(
                                reviewsByBusiness.values().stream().filter(list -> !list.isEmpty()).count())),
                        new ReportDtos.Metric("5 star reviews", String.valueOf(fiveStar)),
                        new ReportDtos.Metric("Average rating", decimal(averageReviewRating(
                                businesses.stream()
                                        .flatMap(b -> reviewsByBusiness.getOrDefault(b.getId(), List.of()).stream())
                                        .toList())))));
    }

    /** Report 4 - owner performance. */
    private ReportDtos.ReportView ownerPerformance() {
        List<User> owners = userRepository.findAll().stream()
                .filter(user -> user.getRole() == za.ac.richfield.cbdventures.domain.enums.Role.OWNER)
                .sorted(Comparator.comparing(User::getFullName))
                .toList();
        List<Business> businesses = businessRepository.findAll();
        List<EventListing> events = eventRepository.findAll();
        List<Review> reviews = reviewRepository.findAll();

        Map<Long, List<Business>> byOwner = new LinkedHashMap<>();
        for (Business business : businesses) {
            byOwner.computeIfAbsent(business.getOwner().getId(), id -> new ArrayList<>()).add(business);
        }
        Map<Long, Long> reviewCounts = new LinkedHashMap<>();
        for (Review review : reviews) {
            reviewCounts.merge(review.getBusiness().getId(), 1L, Long::sum);
        }
        Map<Long, Long> eventCounts = new LinkedHashMap<>();
        for (EventListing event : events) {
            eventCounts.merge(event.getBusiness().getId(), 1L, Long::sum);
        }

        List<List<String>> rows = new ArrayList<>();
        long totalLive = 0;
        for (User owner : owners) {
            List<Business> listed = byOwner.getOrDefault(owner.getId(), List.of());
            long live = count(listed, BusinessStatus.ACTIVE);
            totalLive += live;
            long received = listed.stream()
                    .mapToLong(b -> reviewCounts.getOrDefault(b.getId(), 0L)).sum();
            long hosted = listed.stream()
                    .mapToLong(b -> eventCounts.getOrDefault(b.getId(), 0L)).sum();
            rows.add(List.of(
                    owner.getFullName(),
                    owner.getEmail(),
                    String.valueOf(listed.size()),
                    String.valueOf(live),
                    String.valueOf(count(listed, BusinessStatus.PENDING)),
                    String.valueOf(hosted),
                    String.valueOf(received),
                    decimal(averageRating(listed))));
        }

        return new ReportDtos.ReportView(
                "owner-performance",
                "Owner performance",
                "What each business owner has listed, how much of it is live and the reviews they earned.",
                now(),
                List.of("Owner", "Email", "Listings", "Live", "Pending", "Events", "Reviews received",
                        "Average rating"),
                rows,
                List.of(
                        new ReportDtos.Metric("Owners", String.valueOf(owners.size())),
                        new ReportDtos.Metric("Live listings", String.valueOf(totalLive)),
                        new ReportDtos.Metric("Events hosted", String.valueOf(events.size())),
                        new ReportDtos.Metric("Reviews received", String.valueOf(reviews.size()))));
    }

    /** Report 5 - events schedule. */
    private ReportDtos.ReportView eventsSchedule() {
        List<EventListing> events = new ArrayList<>(eventRepository.findAll());
        events.sort(Comparator.comparing(EventListing::getEventDateTime));

        List<List<String>> rows = new ArrayList<>();
        long free = 0;
        long upcoming = 0;
        java.time.LocalDateTime nowLocal = java.time.LocalDateTime.now(ZONE);
        for (EventListing event : events) {
            boolean isFree = event.getTicketPrice() == null
                    || event.getTicketPrice().compareTo(java.math.BigDecimal.ZERO) == 0;
            if (isFree) {
                free++;
            }
            if (event.getEventDateTime().isAfter(nowLocal)) {
                upcoming++;
            }
            rows.add(List.of(
                    event.getTitle(),
                    event.getBusiness().getName(),
                    blankToDash(event.getVenue()),
                    event.getEventDateTime().format(DATE_TIME),
                    isFree ? "Free" : "R" + event.getTicketPrice().stripTrailingZeros().toPlainString(),
                    event.getBusiness().getCategory().getName(),
                    metres(distanceFromAnchor(event.getBusiness()))));
        }

        return new ReportDtos.ReportView(
                "events-schedule",
                "Events schedule",
                "Every event hosted by a listed venue, in date order.",
                now(),
                List.of("Event", "Venue (business)", "Room / area", "Date and time", "Tickets", "Category",
                        "Distance"),
                rows,
                List.of(
                        new ReportDtos.Metric("Events", String.valueOf(events.size())),
                        new ReportDtos.Metric("Still upcoming", String.valueOf(upcoming)),
                        new ReportDtos.Metric("Free events", String.valueOf(free)),
                        new ReportDtos.Metric("Venues", String.valueOf(events.stream()
                                .map(e -> e.getBusiness().getId()).distinct().count()))));
    }

    /* ------------------------------------------------------------------ csv */

    /** Renders a report as RFC-4180 CSV (quoted cells, CRLF line endings). */
    public String toCsv(ReportDtos.ReportView report) {
        StringBuilder csv = new StringBuilder();
        csv.append(csvRow(report.columns()));
        for (List<String> row : report.rows()) {
            csv.append(csvRow(row));
        }
        return csv.toString();
    }

    private String csvRow(List<String> cells) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append('"').append(cells.get(i) == null ? "" : cells.get(i).replace("\"", "\"\"")).append('"');
        }
        return line.append("\r\n").toString();
    }

    /* -------------------------------------------------------------- helpers */

    private long count(List<Business> businesses, BusinessStatus status) {
        return businesses.stream().filter(b -> b.getStatus() == status).count();
    }

    private long reviewCount(List<Business> businesses) {
        return businesses.stream()
                .mapToLong(b -> reviewRepository.countByBusinessId(b.getId()))
                .sum();
    }

    private double averageRating(List<Business> businesses) {
        List<Double> values = businesses.stream()
                .map(b -> averageReviewRating(reviewRepository.findByBusinessIdOrderByCreatedAtDesc(b.getId())))
                .filter(value -> value > 0)
                .toList();
        if (values.isEmpty()) {
            return 0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private double averageReviewRating(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return 0;
        }
        return reviews.stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(Review::getRating)
                .average()
                .orElse(0);
    }

    private long starCount(List<Review> reviews, int stars) {
        return reviews.stream().filter(r -> r.getRating() != null && r.getRating() == stars).count();
    }

    private double distanceFromAnchor(Business business) {
        return GeoUtils.distanceKm(
                zoneService.anchorLatitude(), zoneService.anchorLongitude(),
                business.getLatitude().doubleValue(), business.getLongitude().doubleValue());
    }

    private String metres(double km) {
        return Math.round(km * 1000) + " m";
    }

    private String decimal(double value) {
        return value <= 0 ? "—" : String.format(Locale.ENGLISH, "%.1f", value);
    }

    private String humanStatus(BusinessStatus status) {
        return switch (status) {
            case ACTIVE -> "Live";
            case PENDING -> "Awaiting review";
            case REJECTED -> "Rejected";
            case INACTIVE -> "Hidden";
        };
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String localDate(java.time.Instant instant) {
        return instant == null ? "—" : instant.atZone(ZONE).format(DATE);
    }

    private String now() {
        return java.time.Instant.now().atZone(ZONE).format(DATE_TIME);
    }
}
