package za.ac.richfield.cbdventures.service;

import org.springframework.stereotype.Component;
import za.ac.richfield.cbdventures.domain.Business;
import za.ac.richfield.cbdventures.domain.BusinessHours;
import za.ac.richfield.cbdventures.domain.BusinessImage;
import za.ac.richfield.cbdventures.domain.EventListing;
import za.ac.richfield.cbdventures.domain.Review;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.domain.enums.Role;
import za.ac.richfield.cbdventures.repo.ReviewRepository;
import za.ac.richfield.cbdventures.util.GeoUtils;
import za.ac.richfield.cbdventures.web.dto.BusinessCard;
import za.ac.richfield.cbdventures.web.dto.BusinessDetail;
import za.ac.richfield.cbdventures.web.dto.CatalogDtos;
import za.ac.richfield.cbdventures.web.dto.EventDtos;
import za.ac.richfield.cbdventures.web.dto.ReviewDtos;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Converts entities into the view models consumed by the web client. Keeping the
 * mapping in one place guarantees the result list and the profile screen always
 * agree on rating, distance and thumbnail rules.
 */
@Component
public class BusinessMapper {

    private static final ZoneId CBD_ZONE = ZoneId.of("Africa/Johannesburg");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ReviewRepository reviewRepository;
    private final ZoneService zoneService;

    public BusinessMapper(ReviewRepository reviewRepository, ZoneService zoneService) {
        this.reviewRepository = reviewRepository;
        this.zoneService = zoneService;
    }

    /** Average rating and number of reviews for one business. */
    public record Rating(double average, long count) {
        public static final Rating EMPTY = new Rating(0.0, 0L);
    }

    public Map<Long, Rating> ratingsFor(Collection<Business> businesses) {
        List<Long> ids = businesses.stream().map(Business::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Rating> ratings = new HashMap<>();
        for (Object[] row : reviewRepository.summariseForBusinesses(ids)) {
            Long businessId = ((Number) row[0]).longValue();
            double average = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
            long count = row[2] == null ? 0L : ((Number) row[2]).longValue();
            ratings.put(businessId, new Rating(Math.round(average * 10.0) / 10.0, count));
        }
        return ratings;
    }

    public BusinessCard toCard(Business business, Rating rating, Double referenceLat, Double referenceLng) {
        Double distance = null;
        Integer walking = null;
        if (referenceLat != null && referenceLng != null) {
            double km = GeoUtils.distanceKm(referenceLat, referenceLng,
                    business.getLatitude().doubleValue(), business.getLongitude().doubleValue());
            distance = GeoUtils.round2(km);
            walking = GeoUtils.walkingMinutes(km);
        }
        Rating safeRating = rating == null ? Rating.EMPTY : rating;
        return new BusinessCard(
                business.getId(),
                business.getName(),
                business.getCategory().getId(),
                business.getCategory().getName(),
                business.getCategory().getSlug(),
                business.getCategory().getIconKey(),
                business.getPriceLevel().name(),
                business.getPriceLevel().label(),
                safeRating.average(),
                safeRating.count(),
                distance,
                walking,
                business.getAddress(),
                business.getOperatingHours(),
                todayHours(business),
                openNow(business),
                thumbnail(business),
                business.getAmenities().stream().map(a -> a.getName()).limit(4).toList(),
                business.getStatus().name());
    }

    public BusinessDetail toDetail(Business business, User viewer, Rating rating) {
        List<Review> reviews = reviewRepository.findByBusinessIdOrderByCreatedAtDesc(business.getId());
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (int stars = 5; stars >= 1; stars--) {
            distribution.put(String.valueOf(stars), 0L);
        }
        for (Review review : reviews) {
            distribution.merge(String.valueOf(review.getRating()), 1L, Long::sum);
        }

        boolean signedIn = viewer != null;
        boolean isOwner = signedIn && business.getOwner().getId().equals(viewer.getId());
        boolean isAdmin = signedIn && viewer.getRole() == Role.ADMIN;
        boolean mine = isOwner || isAdmin;
        ReviewDtos.ReviewView myReview = signedIn
                ? reviews.stream()
                .filter(r -> r.getUser().getId().equals(viewer.getId()))
                .findFirst()
                .map(r -> toReviewView(r, viewer))
                .orElse(null)
                : null;

        Rating safeRating = rating == null ? Rating.EMPTY : rating;
        List<EventDtos.EventView> events = business.getEvents().stream()
                .filter(e -> e.getEventDateTime().isAfter(java.time.LocalDateTime.now()))
                .sorted(Comparator.comparing(EventListing::getEventDateTime))
                .map(e -> toEventView(e, business, viewer))
                .toList();

        Double distance = null;
        Integer walking = null;
        if (viewer != null) {
            double km = zoneService.distanceFromAnchor(business.getLatitude().doubleValue(),
                    business.getLongitude().doubleValue());
            distance = GeoUtils.round2(km);
            walking = GeoUtils.walkingMinutes(km);
        }

        return new BusinessDetail(
                business.getId(),
                business.getName(),
                business.getDescription(),
                business.getCategory().getId(),
                business.getCategory().getName(),
                business.getCategory().getSlug(),
                business.getCategory().getIconKey(),
                business.getPriceLevel().name(),
                business.getPriceLevel().label(),
                safeRating.average(),
                safeRating.count(),
                distribution,
                distance,
                walking,
                business.getAddress(),
                business.getLatitude(),
                business.getLongitude(),
                business.getOperatingHours(),
                todayHours(business),
                weekView(business),
                openNow(business),
                business.getPhone(),
                business.getWhatsapp(),
                business.getWebsite(),
                business.getZoneRadius().name(),
                thumbnail(business),
                business.getImages().stream().map(this::toImageView).toList(),
                business.getAmenities().stream()
                        .map(a -> new CatalogDtos.AmenityView(a.getId(), a.getName(), a.getIconKey(),
                                a.getCategory() == null ? null : a.getCategory().getName()))
                        .toList(),
                events,
                reviews.stream().map(r -> toReviewView(r, viewer)).toList(),
                business.getOwner().getFullName(),
                business.getStatus().name(),
                mine,
                signedIn && !isOwner && !isAdmin && myReview == null
                        && business.getStatus().name().equals("ACTIVE"),
                myReview,
                directionsUrl(business),
                business.getCreatedAt(),
                business.getUpdatedAt());
    }

    public ReviewDtos.ReviewView toReviewView(Review review, User viewer) {
        String author = review.getUser().getFullName();
        return new ReviewDtos.ReviewView(
                review.getId(),
                author,
                initials(author),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.isVerified(),
                viewer != null && review.getUser().getId().equals(viewer.getId()));
    }

    public EventDtos.EventView toEventView(EventListing event, Business business, User viewer) {
        Double distance = GeoUtils.round2(zoneService.distanceFromAnchor(
                business.getLatitude().doubleValue(), business.getLongitude().doubleValue()));
        return new EventDtos.EventView(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getVenue(),
                event.getEventDateTime(),
                event.getTicketPrice(),
                event.getTicketPrice() == null || event.getTicketPrice().signum() == 0,
                business.getId(),
                business.getName(),
                business.getAddress(),
                business.getCategory().getName(),
                distance,
                thumbnail(business));
    }

    public BusinessDetail.ImageView toImageView(BusinessImage image) {
        return new BusinessDetail.ImageView(image.getId(), image.getImageUrl(), image.getCaption(), image.isPrimary());
    }

    /** Primary gallery image, otherwise a generated category illustration. */
    public String thumbnail(Business business) {
        return business.getImages().stream()
                .filter(BusinessImage::isPrimary)
                .map(BusinessImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> business.getImages().stream()
                        .findFirst()
                        .map(BusinessImage::getImageUrl)
                        .orElse("/images/covers/" + business.getCategory().getSlug() + "-01.svg"));
    }

    public static String directionsUrl(Business business) {
        return "https://www.google.com/maps/dir/?api=1&destination="
                + business.getLatitude().toPlainString() + "," + business.getLongitude().toPlainString();
    }

    /**
     * "Is it open right now?"
     *
     * <p>Per-day {@code business_hours} win when present (adopted from the group's
     * design, so "closed on Sundays" is possible). When a listing only has the
     * legacy single window, that is used as a fallback. Returns {@code null} when
     * there is no hours information at all, so the UI can say "hours unknown"
     * rather than wrongly claiming the venue is closed.</p>
     */
    public Boolean openNow(Business business) {
        ZonedDateTime now = ZonedDateTime.now(CBD_ZONE);
        BusinessHours today = todayRow(business, now.getDayOfWeek());
        if (today != null) {
            return today.covers(now.toLocalTime());
        }
        LocalTime open = business.getOpenTime();
        LocalTime close = business.getCloseTime();
        if (open == null || close == null) {
            return null;
        }
        LocalTime time = now.toLocalTime();
        if (close.isAfter(open)) {
            return !time.isBefore(open) && time.isBefore(close);
        }
        // Hours that run past midnight, e.g. 18:00 - 02:00
        return !time.isBefore(open) || time.isBefore(close);
    }

    /** The row for today, or null when this business has no hours rows at all. */
    private BusinessHours todayRow(Business business, DayOfWeek day) {
        if (business.getHours() == null || business.getHours().isEmpty()) {
            return null;
        }
        return business.hoursFor(day);
    }

    /** One-line summary for cards: "07:00 - 17:00", "Closed today" or null. */
    public String todayHours(Business business) {
        ZonedDateTime now = ZonedDateTime.now(CBD_ZONE);
        if (business.getHours() == null || business.getHours().isEmpty()) {
            LocalTime open = business.getOpenTime();
            LocalTime close = business.getCloseTime();
            if (open == null || close == null) {
                return null;
            }
            return open.format(TIME) + " - " + close.format(TIME);
        }
        BusinessHours today = business.hoursFor(now.getDayOfWeek());
        if (today == null) {
            return null;
        }
        if (today.isClosed()) {
            return "Closed today";
        }
        return today.getOpenTime().format(TIME) + " - " + today.getCloseTime().format(TIME);
    }

    /** Full week for the profile screen, Monday first, with today flagged. */
    public List<BusinessDetail.HoursView> weekView(Business business) {
        DayOfWeek today = ZonedDateTime.now(CBD_ZONE).getDayOfWeek();
        List<BusinessHours> rows = business.getHours() == null ? List.of() : business.getHours();
        List<BusinessDetail.HoursView> week = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            BusinessHours row = rows.stream().filter(r -> r.isFor(day)).findFirst().orElse(null);
            boolean isToday = day == today;
            if (row == null) {
                // No information for that day: neither open nor closed.
                week.add(new BusinessDetail.HoursView(day.getValue(), name(day), shortName(day),
                        false, null, null, isToday));
                continue;
            }
            week.add(new BusinessDetail.HoursView(
                    day.getValue(),
                    name(day),
                    shortName(day),
                    row.isClosed(),
                    row.getOpenTime() == null ? null : row.getOpenTime().format(TIME),
                    row.getCloseTime() == null ? null : row.getCloseTime().format(TIME),
                    isToday));
        }
        return week;
    }

    private String name(DayOfWeek day) {
        String lower = day.name().toLowerCase(Locale.ENGLISH);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String shortName(DayOfWeek day) {
        return name(day).substring(0, 3);
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            builder.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return builder.toString();
    }
}
