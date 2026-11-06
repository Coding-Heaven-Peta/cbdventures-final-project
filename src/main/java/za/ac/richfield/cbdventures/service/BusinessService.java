package za.ac.richfield.cbdventures.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.richfield.cbdventures.domain.*;
import za.ac.richfield.cbdventures.domain.enums.ActivityType;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.domain.enums.PriceLevel;
import za.ac.richfield.cbdventures.domain.enums.Role;
import za.ac.richfield.cbdventures.domain.enums.ZoneRadius;
import za.ac.richfield.cbdventures.error.ApiException;
import za.ac.richfield.cbdventures.repo.AmenityRepository;
import za.ac.richfield.cbdventures.repo.BusinessRepository;
import za.ac.richfield.cbdventures.repo.BusinessSpecifications;
import za.ac.richfield.cbdventures.repo.CategoryRepository;
import za.ac.richfield.cbdventures.util.GeoUtils;
import za.ac.richfield.cbdventures.web.dto.BusinessCard;
import za.ac.richfield.cbdventures.web.dto.BusinessDetail;
import za.ac.richfield.cbdventures.web.dto.BusinessRequest;
import za.ac.richfield.cbdventures.web.dto.MapPin;
import za.ac.richfield.cbdventures.web.dto.PageResponse;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Discovery and listing management (FR-01, FR-02, FR-03).
 */
@Service
public class BusinessService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_IMAGES = 8;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final BusinessRepository businessRepository;
    private final CategoryRepository categoryRepository;
    private final AmenityRepository amenityRepository;
    private final BusinessMapper mapper;
    private final ZoneService zoneService;
    private final ActivityService activityService;

    public BusinessService(BusinessRepository businessRepository,
                           CategoryRepository categoryRepository,
                           AmenityRepository amenityRepository,
                           BusinessMapper mapper,
                           ZoneService zoneService,
                           ActivityService activityService) {
        this.businessRepository = businessRepository;
        this.categoryRepository = categoryRepository;
        this.amenityRepository = amenityRepository;
        this.mapper = mapper;
        this.zoneService = zoneService;
        this.activityService = activityService;
    }

    // ------------------------------------------------------------------
    // Discovery
    // ------------------------------------------------------------------

    /**
     * Filtered, sorted search across the pilot zone.
     *
     * <p>Database filters: status, zone, category, price level, keywords.
     * In-memory filters: minimum rating, amenities (all-of), open-now and the
     * distance radius, because these depend on aggregates or on haversine maths.</p>
     */
    @Transactional(readOnly = true)
    public PageResponse<BusinessCard> search(BusinessQuery rawQuery) {
        BusinessQuery query = rawQuery.normalised(MAX_PAGE_SIZE);

        List<Specification<Business>> specifications = java.util.stream.Stream.of(
                        BusinessSpecifications.status(BusinessStatus.ACTIVE),
                        BusinessSpecifications.discoverable(zoneService.extendedEnabled()),
                        BusinessSpecifications.categorySlug(query.category()),
                        BusinessSpecifications.priceLevels(query.priceLevelEnums()),
                        BusinessSpecifications.keywords(query.q()))
                .filter(java.util.Objects::nonNull)
                .toList();

        Specification<Business> spec = specifications.getFirst();
        for (int i = 1; i < specifications.size(); i++) {
            spec = spec.and(specifications.get(i));
        }

        List<Business> candidates = businessRepository.findAll(spec);
        Map<Long, BusinessMapper.Rating> ratings = mapper.ratingsFor(candidates);

        Double refLat = query.latitude() != null ? query.latitude() : zoneService.anchorLatitude();
        Double refLng = query.longitude() != null ? query.longitude() : zoneService.anchorLongitude();
        double radius = query.maxDistanceKm() != null ? query.maxDistanceKm() : zoneService.activeRadiusKm();

        List<Business> filtered = candidates.stream()
                .filter(b -> ratings.getOrDefault(b.getId(), BusinessMapper.Rating.EMPTY).average() >=
                        (query.minRating() == null ? 0.0 : query.minRating()))
                .filter(b -> matchesAmenities(b, query.amenityIds()))
                .filter(b -> query.openNow() == null || !query.openNow() || Boolean.TRUE.equals(mapper.openNow(b)))
                .filter(b -> distance(b, refLat, refLng) <= radius)
                .toList();

        Map<String, Object> applied = new LinkedHashMap<>();
        applied.put("query", query.q());
        applied.put("category", query.category());
        applied.put("priceLevels", query.priceLevelEnums().stream().map(Enum::name).toList());
        applied.put("minRating", query.minRating());
        applied.put("maxDistanceKm", radius);
        applied.put("amenities", amenityNames(query.amenityIds()));
        applied.put("openNow", Boolean.TRUE.equals(query.openNow()));
        applied.put("sort", query.sort());
        applied.put("reference", query.latitude() != null ? "your location" : "Richfield campus");

        List<BusinessCard> cards = sort(filtered, ratings, query, refLat, refLng);

        int from = Math.min(query.page() * query.size(), cards.size());
        int to = Math.min(from + query.size(), cards.size());
        List<BusinessCard> pageItems = cards.subList(from, to);

        return new PageResponse<>(List.copyOf(pageItems), cards.size(), query.page(), query.size(),
                (int) Math.ceil(cards.size() / (double) query.size()), query.sort(), applied, zoneService.view());
    }

    private List<BusinessCard> sort(List<Business> businesses,
                                    Map<Long, BusinessMapper.Rating> ratings,
                                    BusinessQuery query,
                                    Double refLat,
                                    Double refLng) {
        List<BusinessCard> cards = businesses.stream()
                .map(b -> mapper.toCard(b, ratings.get(b.getId()), refLat, refLng))
                .collect(Collectors.toCollection(ArrayList::new));

        Comparator<BusinessCard> comparator = switch (query.sort()) {
            case "distance" -> Comparator.comparing(BusinessCard::distanceKm,
                    Comparator.nullsLast(Double::compareTo));
            case "price" -> Comparator.comparingInt(card -> PriceLevel.valueOf(card.priceLevel()).weight());
            case "rating" -> Comparator.comparingDouble(BusinessCard::rating).reversed()
                    .thenComparing(Comparator.comparingLong(BusinessCard::reviewCount).reversed());
            case "name" -> Comparator.comparing(card -> card.name().toLowerCase());
            default -> Comparator.comparingDouble(BusinessCard::rating).reversed()
                    .thenComparing(Comparator.comparingLong(BusinessCard::reviewCount).reversed());
        };

        if ("recommended".equals(query.sort())) {
            comparator = Comparator
                    .comparingDouble((BusinessCard card) -> Scoring.baseScore(
                            card.rating(), card.reviewCount(), card.distanceKm(),
                            card.thumbnail() != null,
                            card.operatingHours() != null,
                            card.amenities() == null ? 0 : card.amenities().size()))
                    .reversed();
        }

        cards.sort(comparator
                .thenComparing(BusinessCard::name, String.CASE_INSENSITIVE_ORDER));
        return cards;
    }

    private boolean matchesAmenities(Business business, List<Long> amenityIds) {
        if (amenityIds == null || amenityIds.isEmpty()) {
            return true;
        }
        Set<Long> owned = business.getAmenities().stream().map(Amenity::getId).collect(Collectors.toSet());
        return owned.containsAll(amenityIds);
    }

    private List<String> amenityNames(List<Long> amenityIds) {
        if (amenityIds == null || amenityIds.isEmpty()) {
            return List.of();
        }
        return amenityRepository.findByIdIn(amenityIds).stream().map(Amenity::getName).toList();
    }

    private double distance(Business business, Double refLat, Double refLng) {
        return GeoUtils.distanceKm(refLat, refLng,
                business.getLatitude().doubleValue(), business.getLongitude().doubleValue());
    }

    /**
     * FR-02 - every active listing plotted inside the browsing radius, with the
     * walking distance from the campus anchor. Used by the dark-mode map screen.
     */
    @Transactional(readOnly = true)
    public List<MapPin> mapPins() {
        double radius = zoneService.activeRadiusKm();
        List<Business> businesses = businessRepository.findByStatusOrderByCreatedAtDesc(BusinessStatus.ACTIVE);
        Map<Long, BusinessMapper.Rating> ratings = mapper.ratingsFor(businesses);

        return businesses.stream()
                .filter(business -> business.getLatitude() != null && business.getLongitude() != null)
                .map(business -> Map.entry(business, zoneService.distanceFromAnchor(
                        business.getLatitude().doubleValue(),
                        business.getLongitude().doubleValue())))
                .filter(entry -> entry.getValue() <= radius)
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> {
                    Business business = entry.getKey();
                    BusinessMapper.Rating rating = ratings.getOrDefault(business.getId(),
                            BusinessMapper.Rating.EMPTY);
                    return new MapPin(
                            business.getId(),
                            business.getName(),
                            business.getCategory().getName(),
                            business.getCategory().getIconKey(),
                            business.getLatitude().doubleValue(),
                            business.getLongitude().doubleValue(),
                            rating.average(),
                            rating.count(),
                            Math.round(entry.getValue() * 100.0) / 100.0,
                            (int) Math.max(1, Math.round(entry.getValue() * 1000 / 80.0)),
                            business.getPriceLevel().name());
                })
                .toList();
    }

    /** Full profile. Optionally personalised with the signed-in viewer. */
    @Transactional
    public BusinessDetail detail(Long id, User viewer) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("That business could not be found."));
        boolean privileged = viewer != null
                && (viewer.getRole() == Role.ADMIN || business.getOwner().getId().equals(viewer.getId()));
        if (business.getStatus() != BusinessStatus.ACTIVE && !privileged) {
            throw ApiException.notFound("That business could not be found.");
        }
        Map<Long, BusinessMapper.Rating> ratings = mapper.ratingsFor(List.of(business));
        BusinessDetail detail = mapper.toDetail(business, viewer, ratings.get(business.getId()));
        // Browsing trail for recommendations; a no-op for guests and for repeat views.
        if (viewer != null && business.getOwner().getId() != viewer.getId()) {
            activityService.record(viewer, business, ActivityType.VIEW_PROFILE);
        }
        return detail;
    }

    @Transactional(readOnly = true)
    public Business businessEntity(Long id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("That business could not be found."));
    }

    // ------------------------------------------------------------------
    // Business owner management (FR-03)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BusinessCard> myListings(User owner) {
        List<Business> mine = businessRepository.findByOwnerIdOrderByNameAsc(owner.getId());
        Map<Long, BusinessMapper.Rating> ratings = mapper.ratingsFor(mine);
        return mine.stream()
                .map(b -> mapper.toCard(b, ratings.get(b.getId()),
                        zoneService.anchorLatitude(), zoneService.anchorLongitude()))
                .toList();
    }

    @Transactional
    public BusinessDetail create(User owner, BusinessRequest request) {
        requireOwnerRole(owner);
        Business business = new Business();
        business.setOwner(owner);
        apply(business, request);
        // New listings wait for administrator verification (Phase 3 business rule).
        business.setStatus(BusinessStatus.PENDING);
        business.setAdminNote("Awaiting verification by the CBDVentures team.");
        Business saved = businessRepository.save(business);
        Map<Long, BusinessMapper.Rating> ratings = mapper.ratingsFor(List.of(saved));
        return mapper.toDetail(saved, owner, ratings.get(saved.getId()));
    }

    @Transactional
    public BusinessDetail update(User actor, Long id, BusinessRequest request) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("That business could not be found."));
        boolean isAdmin = actor.getRole() == Role.ADMIN;
        if (!isAdmin && !business.getOwner().getId().equals(actor.getId())) {
            throw ApiException.forbidden("You can only edit your own listings.");
        }
        apply(business, request);
        if (Boolean.TRUE.equals(request.reactivate())) {
            business.setStatus(BusinessStatus.PENDING);
            business.setAdminNote("Re-submitted for verification.");
        } else if (business.getStatus() == BusinessStatus.REJECTED) {
            business.setStatus(BusinessStatus.PENDING);
            business.setAdminNote("Re-submitted for verification.");
        }
        Business saved = businessRepository.save(business);
        Map<Long, BusinessMapper.Rating> ratings = mapper.ratingsFor(List.of(saved));
        return mapper.toDetail(saved, actor, ratings.get(saved.getId()));
    }

    @Transactional
    public void delete(User actor, Long id) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("That business could not be found."));
        boolean isAdmin = actor.getRole() == Role.ADMIN;
        if (!isAdmin && !business.getOwner().getId().equals(actor.getId())) {
            throw ApiException.forbidden("You can only remove your own listings.");
        }
        // Cascade removes images, events and reviews (child entities have no
        // independent purpose without their parent - Phase 3 referential rules).
        businessRepository.delete(business);
    }

    @Transactional
    public void setStatus(User actor, Long id, BusinessStatus status, String note) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("That business could not be found."));
        boolean isAdmin = actor.getRole() == Role.ADMIN;
        if (!isAdmin && !business.getOwner().getId().equals(actor.getId())) {
            throw ApiException.forbidden("You can only change your own listings.");
        }
        business.setStatus(status);
        business.setAdminNote(note);
        businessRepository.save(business);
    }

    private void requireOwnerRole(User user) {
        if (user.getRole() != Role.OWNER && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden(
                    "Business owner accounts can manage listings. Register a business owner account to continue.");
        }
    }

    /** Maps and validates a request onto the entity. */
    private void apply(Business business, BusinessRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ApiException.badRequest("Please choose one of the five supported categories."));

        ZoneRadius radius = zoneService.classify(request.latitude().doubleValue(),
                request.longitude().doubleValue());

        LocalTime open = parseTime(request.openTime(), "opening time");
        LocalTime close = parseTime(request.closeTime(), "closing time");
        if ((open == null) != (close == null)) {
            throw ApiException.badRequest("Please provide both the opening and closing time, or leave both blank.");
        }
        if (open != null && open.equals(close)) {
            throw ApiException.badRequest("The opening time must be earlier than the closing time.");
        }

        // Per-day hours (optional). Validated fully before anything is mutated.
        List<BusinessHours> week = request.hours() == null || request.hours().isEmpty()
                ? List.of()
                : buildHours(request.hours());

        business.setName(request.name().trim());
        business.setDescription(request.description() == null ? null : request.description().trim());
        business.setAddress(request.address().trim());
        business.setLatitude(scale(request.latitude(), 8));
        business.setLongitude(scale(request.longitude(), 8));
        business.setCategory(category);
        business.setPriceLevel(request.priceLevel());
        business.setZoneRadius(radius);
        business.setPhone(trimToNull(request.phone()));
        business.setWhatsapp(trimToNull(request.whatsapp()));
        business.setWebsite(trimToNull(request.website()));
        business.setOpenTime(open);
        business.setCloseTime(close);
        if (week.isEmpty()) {
            // No per-day hours supplied: keep the legacy single window in charge.
            business.setOperatingHours(operatingHours(request.operatingHours(), open, close));
        } else {
            business.replaceHours(week);
            if (request.operatingHours() != null && !request.operatingHours().isBlank()) {
                business.setOperatingHours(request.operatingHours().trim());
            } else {
                business.setOperatingHours(summarise(week));
            }
            // A Monday-first window is also mirrored into the legacy columns so older
            // reports and CSV exports keep working.
            BusinessHours monday = week.stream().filter(r -> r.isFor(DayOfWeek.MONDAY)).findFirst().orElse(null);
            if (monday != null && !monday.isClosed()) {
                business.setOpenTime(monday.getOpenTime());
                business.setCloseTime(monday.getCloseTime());
            }
        }

        Set<Amenity> amenities = request.amenityIds() == null || request.amenityIds().isEmpty()
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(amenityRepository.findByIdIn(request.amenityIds()));
        business.setAmenities(amenities);

        applyImages(business, request.imageUrls());

        if (business.getStatus() == BusinessStatus.ACTIVE && radius == ZoneRadius.EXTENDED_5KM
                && !zoneService.extendedEnabled()) {
            business.setStatus(BusinessStatus.PENDING);
            business.setAdminNote("Saved outside the 1 km primary zone - it will publish when the "
                    + "extended zone opens.");
        }
    }

    /**
     * Turns the request's per-day rows into entities, enforcing the domain rules
     * the database also enforces through triggers/constraints:
     * <ul>
     *   <li>no duplicate weekday,</li>
     *   <li>a day is either closed (no times) or open (both times present),</li>
     *   <li>opening time strictly before closing time.</li>
     * </ul>
     */
    private List<BusinessHours> buildHours(List<BusinessRequest.HoursRequest> rows) {
        List<BusinessHours> week = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        for (BusinessRequest.HoursRequest row : rows) {
            if (row == null || row.dayOfWeek() == null) {
                throw ApiException.badRequest("Every opening-hours row must name a day of the week (1 = Monday to 7 = Sunday).");
            }
            if (!seen.add(row.dayOfWeek())) {
                throw ApiException.badRequest("The opening hours list contains the day "
                        + DayOfWeek.of(row.dayOfWeek()) + " twice. Please list each day once.");
            }
            DayOfWeek day = DayOfWeek.of(row.dayOfWeek());
            boolean closed = Boolean.TRUE.equals(row.closed());
            LocalTime open = parseTime(row.openTime(), day + " opening time");
            LocalTime close = parseTime(row.closeTime(), day + " closing time");

            if (closed) {
                if (open != null || close != null) {
                    throw ApiException.badRequest("Because " + day
                            + " is marked as a closed day it cannot have opening or closing times.");
                }
                week.add(BusinessHours.closed(day));
                continue;
            }
            if (open == null || close == null) {
                throw ApiException.badRequest("Please give both an opening and a closing time for "
                        + day + ", or mark the day as closed.");
            }
            if (!open.isBefore(close)) {
                throw ApiException.badRequest("On " + day
                        + ", the opening time must be earlier than the closing time.");
            }
            week.add(BusinessHours.open(day, open, close));
        }
        week.sort(Comparator.comparing(BusinessHours::getDayOfWeek));
        return week;
    }

    /** Human summary for the CSVs and legacy cards, e.g. "Mon-Thu 07:00-17:00, Fri 07:00-19:00, Sun closed". */
    private String summarise(List<BusinessHours> week) {
        List<String> parts = new ArrayList<>();
        for (BusinessHours row : week) {
            String label = row.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            parts.add(row.isClosed()
                    ? label + " closed"
                    : label + " " + row.getOpenTime().format(TIME_FORMAT) + "-" + row.getCloseTime().format(TIME_FORMAT));
        }
        return String.join(", ", parts);
    }

    private void applyImages(Business business, List<String> imageUrls) {
        List<String> clean = imageUrls == null ? List.of() : imageUrls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .distinct()
                .limit(MAX_IMAGES)
                .toList();

        business.getImages().clear();
        for (int i = 0; i < clean.size(); i++) {
            // Exactly one photo is the primary thumbnail: the first one supplied.
            business.addImage(new BusinessImage(clean.get(i), null, i == 0));
        }
    }

    private String operatingHours(String provided, LocalTime open, LocalTime close) {
        if (provided != null && !provided.isBlank()) {
            return provided.trim();
        }
        if (open != null && close != null) {
            return "Daily " + open.format(TIME_FORMAT) + " - " + close.format(TIME_FORMAT);
        }
        return null;
    }

    private LocalTime parseTime(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim(), TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            throw ApiException.badRequest("The " + label + " must use the 24-hour HH:mm format, e.g. 07:30.");
        }
    }

    private BigDecimal scale(BigDecimal value, int decimals) {
        return value.setScale(decimals, java.math.RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
