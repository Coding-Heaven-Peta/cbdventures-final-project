package za.ac.richfield.cbdventures.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.richfield.cbdventures.domain.*;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.error.ApiException;
import za.ac.richfield.cbdventures.repo.*;
import za.ac.richfield.cbdventures.util.GeoUtils;
import za.ac.richfield.cbdventures.web.dto.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Personalised suggestions (FR-04). Recommendations blend saved preferences,
 * the categories a visitor has already reviewed, rating quality, proximity to
 * the reference point and listing completeness. Guests receive a popularity
 * based feed instead, so the home screen is never empty.
 */
@Service
public class RecommendationService {

    private final BusinessRepository businessRepository;
    private final ReviewRepository reviewRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final CategoryRepository categoryRepository;
    private final BusinessMapper mapper;
    private final ZoneService zoneService;
    private final EventService eventService;
    private final ActivityService activityService;

    public RecommendationService(BusinessRepository businessRepository,
                                 ReviewRepository reviewRepository,
                                 UserPreferenceRepository preferenceRepository,
                                 CategoryRepository categoryRepository,
                                 BusinessMapper mapper,
                                 ZoneService zoneService,
                                 EventService eventService,
                                 ActivityService activityService) {
        this.businessRepository = businessRepository;
        this.reviewRepository = reviewRepository;
        this.preferenceRepository = preferenceRepository;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
        this.zoneService = zoneService;
        this.eventService = eventService;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public HomeResponse home(User viewer, Double latitude, Double longitude) {
        List<Business> active = discoverableBusinesses();
        Map<Long, BusinessMapper.Rating> ratings = mapper.ratingsFor(active);

        Double refLat = latitude != null ? latitude : zoneService.anchorLatitude();
        Double refLng = longitude != null ? longitude : zoneService.anchorLongitude();

        List<BusinessCard> cards = active.stream()
                .map(b -> mapper.toCard(b, ratings.get(b.getId()), refLat, refLng))
                .toList();

        List<BusinessCard> featured = cards.stream()
                .sorted(Comparator.comparingDouble((BusinessCard card) -> Scoring.baseScore(
                        card.rating(), card.reviewCount(), card.distanceKm(),
                        card.thumbnail() != null, card.operatingHours() != null,
                        card.amenities().size())).reversed())
                .limit(6)
                .toList();

        Map<Long, Double> affinity = affinityWeights(viewer);
        boolean personalised = viewer != null && !affinity.isEmpty();

        List<BusinessCard> recommended = cards.stream()
                .sorted(Comparator.comparingDouble((BusinessCard card) -> Scoring.personalised(
                                Scoring.baseScore(card.rating(), card.reviewCount(), card.distanceKm(),
                                        card.thumbnail() != null, card.operatingHours() != null,
                                        card.amenities().size()),
                                Scoring.affinityFor(affinity, card.categoryId())))
                        .reversed())
                .limit(6)
                .toList();

        List<CatalogDtos.CategoryView> categories = categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(category -> new CatalogDtos.CategoryView(
                        category.getId(), category.getName(), category.getSlug(), category.getDescription(),
                        category.getIconKey(),
                        active.stream().filter(b -> b.getCategory().getId().equals(category.getId())).count()))
                .toList();

        List<EventDtos.EventView> events = eventService.upcoming(null, null, 4);

        double averageRating = ratings.values().stream()
                .filter(r -> r.count() > 0)
                .mapToDouble(BusinessMapper.Rating::average)
                .average()
                .orElse(0.0);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("businesses", active.size());
        stats.put("reviews", reviewRepository.count());
        stats.put("categories", categories.size());
        stats.put("events", events.size());
        stats.put("averageRating", Math.round(averageRating * 10.0) / 10.0);
        stats.put("radiusKm", zoneService.activeRadiusKm());

        Map<String, Object> personalisation = new LinkedHashMap<>();
        personalisation.put("personalised", personalised);
        personalisation.put("basis", personalised
                ? "Based on the categories you save, review and browse"
                : "What's popular near Richfield right now");

        return new HomeResponse(zoneService.view(), categories, featured, recommended, events, stats, personalisation);
    }

    @Transactional(readOnly = true)
    public List<BusinessCard> recommendations(User viewer, Double latitude, Double longitude, Integer limit) {
        int max = limit == null ? 6 : Math.min(Math.max(limit, 1), 30);
        List<Business> active = discoverableBusinesses();
        Map<Long, BusinessMapper.Rating> ratings = mapper.ratingsFor(active);
        Double refLat = latitude != null ? latitude : zoneService.anchorLatitude();
        Double refLng = longitude != null ? longitude : zoneService.anchorLongitude();
        Map<Long, Double> affinity = affinityWeights(viewer);

        return active.stream()
                .map(b -> mapper.toCard(b, ratings.get(b.getId()), refLat, refLng))
                .sorted(Comparator.comparingDouble((BusinessCard card) -> Scoring.personalised(
                                Scoring.baseScore(card.rating(), card.reviewCount(), card.distanceKm(),
                                        card.thumbnail() != null, card.operatingHours() != null,
                                        card.amenities().size()),
                                Scoring.affinityFor(affinity, card.categoryId())))
                        .reversed())
                .limit(max)
                .toList();
    }

    /** Category weights learned from saved preferences, past reviews and recent browsing. */
    private Map<Long, Double> affinityWeights(User viewer) {
        Map<Long, Double> weights = new HashMap<>();
        if (viewer == null) {
            return weights;
        }
        preferenceRepository.findByUser(viewer).ifPresent(preference ->
                preference.getPreferredCategories().forEach(category ->
                        weights.merge(category.getId(), 3.0, Double::sum)));

        reviewRepository.findAll().stream()
                .filter(review -> review.getUser().getId().equals(viewer.getId()))
                .forEach(review -> weights.merge(review.getBusiness().getCategory().getId(),
                        review.getRating() >= 4 ? 1.5 : 0.5, Double::sum));

        // user_activity: what the visitor actually browsed in the last 60 days.
        activityService.affinityWeights(viewer).forEach((categoryId, weight) ->
                weights.merge(categoryId, weight, Double::sum));
        return weights;
    }

    private List<Business> discoverableBusinesses() {
        List<Business> active = businessRepository.findByStatusOrderByCreatedAtDesc(BusinessStatus.ACTIVE);
        if (zoneService.extendedEnabled()) {
            return active;
        }
        return active.stream()
                .filter(b -> b.getZoneRadius() == za.ac.richfield.cbdventures.domain.enums.ZoneRadius.PRIMARY_1KM)
                .toList();
    }

    // ------------------------------------------------------------------
    // Saved preferences
    // ------------------------------------------------------------------

    @Transactional
    public AdminDtos.PreferenceView preferences(User viewer) {
        UserPreference preference = preferenceRepository.findByUser(viewer)
                .orElseGet(() -> new UserPreference(viewer));
        return toView(preference);
    }

    @Transactional
    public AdminDtos.PreferenceView savePreferences(User viewer, AdminDtos.PreferenceRequest request) {
        UserPreference preference = preferenceRepository.findByUser(viewer)
                .orElseGet(() -> new UserPreference(viewer));

        preference.setMaxPriceLevel(request.maxPriceLevel());
        preference.setMaxDistanceKm(request.maxDistanceKm());
        preference.setUpdatedAt(java.time.Instant.now());

        List<Category> categories = request.preferredCategoryIds() == null || request.preferredCategoryIds().isEmpty()
                ? List.of()
                : categoryRepository.findAllById(request.preferredCategoryIds());
        if (request.preferredCategoryIds() != null && !request.preferredCategoryIds().isEmpty()
                && categories.size() != request.preferredCategoryIds().size()) {
            throw ApiException.badRequest("One of the selected categories no longer exists.");
        }
        preference.getPreferredCategories().clear();
        preference.getPreferredCategories().addAll(categories);
        return toView(preferenceRepository.save(preference));
    }

    private AdminDtos.PreferenceView toView(UserPreference preference) {
        List<Long> ids = preference.getPreferredCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toList());
        return new AdminDtos.PreferenceView(preference.getMaxPriceLevel(), preference.getMaxDistanceKm(),
                ids, !ids.isEmpty() || preference.getMaxPriceLevel() != null);
    }

    /** Distance from the campus anchor - surfaced on the business profile. */
    public double distanceFromAnchor(Business business) {
        return GeoUtils.round2(zoneService.distanceFromAnchor(
                business.getLatitude().doubleValue(), business.getLongitude().doubleValue()));
    }
}
