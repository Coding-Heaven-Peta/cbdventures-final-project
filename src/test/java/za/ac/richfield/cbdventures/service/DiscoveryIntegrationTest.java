package za.ac.richfield.cbdventures.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import za.ac.richfield.cbdventures.domain.*;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.domain.enums.PriceLevel;
import za.ac.richfield.cbdventures.domain.enums.Role;
import za.ac.richfield.cbdventures.error.ApiException;
import za.ac.richfield.cbdventures.repo.*;
import za.ac.richfield.cbdventures.web.dto.BusinessCard;
import za.ac.richfield.cbdventures.web.dto.BusinessDetail;
import za.ac.richfield.cbdventures.web.dto.HomeResponse;
import za.ac.richfield.cbdventures.web.dto.PageResponse;
import za.ac.richfield.cbdventures.web.dto.ReviewDtos;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4 — integration tests covering the discovery use cases defined in the
 * SRS: search, browse by category, filter by price/rating/proximity, sort,
 * view a profile, and leave a review (with the business rules attached).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DiscoveryIntegrationTest {

    @Autowired CategoryRepository categoryRepository;
    @Autowired AmenityRepository amenityRepository;
    @Autowired BusinessRepository businessRepository;
    @Autowired ReviewRepository reviewRepository;
    @Autowired UserRepository userRepository;
    @Autowired BusinessService businessService;
    @Autowired ReviewService reviewService;
    @Autowired RecommendationService recommendationService;
    @Autowired AdminService adminService;

    private Category food;
    private Category salons;
    private User owner;
    private User visitor;
    private Business kota;
    private Business barber;
    private Business farAway;

    @BeforeEach
    void setUp() {
        food = categoryRepository.save(new Category("Food & Drinks", "food-drinks", "Quick eats", "food", 1));
        salons = categoryRepository.save(new Category("Salons & Personal Care", "salons", "Hair", "salon", 2));

        owner = userRepository.save(new User("Test Owner", "owner@test.co.za", "{bcrypt}", Role.OWNER));
        visitor = userRepository.save(new User("Test Visitor", "visitor@test.co.za", "{bcrypt}", Role.VISITOR));

        kota = businessRepository.save(business("Kota Corner", food, -25.7465, 28.1927,
                PriceLevel.R, BusinessStatus.ACTIVE));
        barber = businessRepository.save(business("Fresh Fade", salons, -25.7500, 28.1960,
                PriceLevel.RR, BusinessStatus.ACTIVE));
        farAway = businessRepository.save(business("Hidden Pending Place", food, -25.7470, 28.1930,
                PriceLevel.R, BusinessStatus.PENDING));
    }

    private Business business(String name, Category category, double lat, double lng,
                              PriceLevel price, BusinessStatus status) {
        Business business = new Business();
        business.setName(name);
        business.setOwner(owner);
        business.setCategory(category);
        business.setAddress(name + " Street, Pretoria Central");
        business.setDescription("Test listing for " + name);
        business.setLatitude(BigDecimal.valueOf(lat));
        business.setLongitude(BigDecimal.valueOf(lng));
        business.setPriceLevel(price);
        business.setStatus(status);
        business.setOperatingHours("Mon-Sat 08:00 - 17:00");
        return business;
    }

    @Test
    @DisplayName("Public search only returns active listings")
    void searchOnlyReturnsActiveListings() {
        PageResponse<BusinessCard> results = businessService.search(BusinessQuery.defaults());
        List<String> names = results.items().stream().map(BusinessCard::name).toList();
        assertTrue(names.contains("Kota Corner"));
        assertFalse(names.contains("Hidden Pending Place"), "pending listings must not be discoverable");
    }

    @Test
    @DisplayName("Browsing by category filters to that category only")
    void browseByCategory() {
        BusinessQuery query = new BusinessQuery(null, "salons", null, null, null, null, null,
                "recommended", 0, 24, null, null);
        PageResponse<BusinessCard> results = businessService.search(query);
        assertEquals(1, results.total());
        assertEquals("Fresh Fade", results.items().getFirst().name());
    }

    @Test
    @DisplayName("Keyword search matches name, description and address")
    void keywordSearch() {
        BusinessQuery query = new BusinessQuery("fade", null, null, null, null, null, null,
                "recommended", 0, 24, null, null);
        assertEquals(1, businessService.search(query).total());
    }

    @Test
    @DisplayName("Price filter keeps only the requested price levels")
    void priceFilter() {
        BusinessQuery query = new BusinessQuery(null, null, List.of(1L), null, null, null, null,
                "price", 0, 24, null, null);
        PageResponse<BusinessCard> results = businessService.search(query);
        assertEquals(1, results.total());
        assertEquals("R", results.items().getFirst().priceLevel());
    }

    @Test
    @DisplayName("Distance filter excludes listings beyond the requested radius")
    void distanceFilter() {
        // 250 m around the campus anchor keeps the kota (≈40 m) but excludes the barber (≈500 m)
        BusinessQuery query = new BusinessQuery(null, null, null, null, 0.25, null, null,
                "distance", 0, 24, null, null);
        PageResponse<BusinessCard> results = businessService.search(query);
        assertEquals(1, results.total());
        assertEquals("Kota Corner", results.items().getFirst().name());
        assertNotNull(results.items().getFirst().distanceKm());
        assertTrue(results.items().getFirst().walkingMinutes() >= 1);
    }

    @Test
    @DisplayName("Sorting by distance orders closest first")
    void sortByDistance() {
        BusinessQuery query = new BusinessQuery(null, null, null, null, null, null, null,
                "distance", 0, 24, null, null);
        List<Double> distances = businessService.search(query).items().stream()
                .map(BusinessCard::distanceKm).toList();
        assertEquals(distances.stream().sorted().toList(), distances);
    }

    @Test
    @DisplayName("Sorting by price orders cheapest first")
    void sortByPrice() {
        BusinessQuery query = new BusinessQuery(null, null, null, null, null, null, null,
                "price", 0, 24, null, null);
        assertEquals(List.of("R", "RR"), businessService.search(query).items().stream()
                .map(BusinessCard::priceLevel).toList());
    }

    @Test
    @DisplayName("A reviewer can post once, and the average rating is recalculated")
    void reviewRecalculatesAverage() {
        reviewService.upsert(visitor, kota.getId(), new ReviewDtos.ReviewRequest(5, "Excellent value."));
        assertEquals(5.0, reviewService.averageFor(kota.getId()), 1e-9);
        assertEquals(1, reviewService.countFor(kota.getId()));

        BusinessDetail detail = businessService.detail(kota.getId(), visitor);
        assertEquals(5.0, detail.rating(), 1e-9);
        assertEquals(1L, detail.ratingDistribution().get("5"));
        assertNotNull(detail.myReview());
        assertFalse(detail.canReview(), "an existing reviewer must not be offered a second review");
    }

    @Test
    @DisplayName("A second review inside 24 hours is rejected as spam")
    void reviewCooldown() {
        reviewService.upsert(visitor, kota.getId(), new ReviewDtos.ReviewRequest(4, "Good."));
        ApiException error = assertThrows(ApiException.class,
                () -> reviewService.upsert(visitor, kota.getId(), new ReviewDtos.ReviewRequest(1, "Changed my mind.")));
        assertTrue(error.getMessage().contains("24 hours"));
    }

    @Test
    @DisplayName("An owner cannot review their own business")
    void ownerCannotReviewOwnBusiness() {
        ApiException error = assertThrows(ApiException.class,
                () -> reviewService.upsert(owner, kota.getId(), new ReviewDtos.ReviewRequest(5, "Self review")));
        assertTrue(error.getMessage().contains("cannot review a business you manage"));
    }

    @Test
    @DisplayName("Ratings outside 1 to 5 are rejected")
    void ratingRangeValidation() {
        assertThrows(ApiException.class,
                () -> reviewService.upsert(visitor, kota.getId(), new ReviewDtos.ReviewRequest(6, "Too high")));
        assertThrows(ApiException.class,
                () -> reviewService.upsert(visitor, kota.getId(), new ReviewDtos.ReviewRequest(0, "Too low")));
    }

    @Test
    @DisplayName("A pending listing cannot be reviewed until it is live")
    void pendingListingCannotBeReviewed() {
        ApiException error = assertThrows(ApiException.class,
                () -> reviewService.upsert(visitor, farAway.getId(), new ReviewDtos.ReviewRequest(5, "Great")));
        assertTrue(error.getMessage().contains("Reviews open once this listing is live"));
    }

    @Test
    @DisplayName("Approving a pending listing publishes it to search")
    void adminApprovalPublishesListing() {
        User admin = userRepository.save(new User("Admin", "admin@test.co.za", "{bcrypt}", Role.ADMIN));
        adminService.decide(admin, farAway.getId(), BusinessStatus.ACTIVE, "Verified in person.");

        BusinessQuery query = new BusinessQuery("Hidden", null, null, null, null, null, null,
                "recommended", 0, 24, null, null);
        assertEquals(1, businessService.search(query).total());
    }

    @Test
    @DisplayName("Home payload returns five categories, suggestions and events")
    void homePayload() {
        HomeResponse home = recommendationService.home(null, null, null);
        assertNotNull(home.zone());
        assertFalse(home.categories().isEmpty(), "the five curated categories must be present");
        assertFalse(home.recommended().isEmpty());
        assertNotNull(home.stats().get("businesses"));
        assertFalse((Boolean) home.personalisation().get("personalised"),
                "guests are not personalised");
    }

    @Test
    @DisplayName("Saved preferences make the feed personalised")
    void preferencesPersonaliseFeed() {
        recommendationService.savePreferences(visitor, new za.ac.richfield.cbdventures.web.dto.AdminDtos.PreferenceRequest(
                PriceLevel.R, 1.0, List.of(salons.getId())));
        HomeResponse home = recommendationService.home(visitor, null, null);
        assertTrue((Boolean) home.personalisation().get("personalised"));
        assertEquals("Fresh Fade", home.recommended().getFirst().name(),
                "the preferred category should surface first");
    }

    @Test
    @DisplayName("Amenity filter requires every requested amenity")
    void amenityFilter() {
        Amenity wifi = amenityRepository.save(new Amenity(food, "Free Wi-Fi", "wifi"));
        Amenity power = amenityRepository.save(new Amenity(food, "Backup Power", "power"));
        barber.getAmenities().add(wifi);
        barber.getAmenities().add(power);
        kota.getAmenities().add(wifi);
        businessRepository.saveAll(List.of(barber, kota));

        BusinessQuery both = new BusinessQuery(null, null, null, null, null,
                List.of(wifi.getId(), power.getId()), null, "recommended", 0, 24, null, null);
        assertEquals(1, businessService.search(both).total());
        assertEquals("Fresh Fade", businessService.search(both).items().getFirst().name());

        BusinessQuery onlyWifi = new BusinessQuery(null, null, null, null, null,
                List.of(wifi.getId()), null, "recommended", 0, 24, null, null);
        assertEquals(2, businessService.search(onlyWifi).total());
    }

    @Test
    @DisplayName("Minimum rating filter hides listings without enough stars")
    void minRatingFilter() {
        reviewService.upsert(visitor, kota.getId(), new ReviewDtos.ReviewRequest(5, "Top marks"));
        BusinessQuery query = new BusinessQuery(null, null, null, 4.5, null, null, null,
                "rating", 0, 24, null, null);
        PageResponse<BusinessCard> results = businessService.search(query);
        assertEquals(1, results.total());
        assertEquals("Kota Corner", results.items().getFirst().name());
    }

    @Test
    @DisplayName("Reviews cannot exist without a user and a business")
    void reviewsAreLinked() {
        long before = reviewRepository.count();
        reviewService.upsert(visitor, kota.getId(), new ReviewDtos.ReviewRequest(4, "Linked"));
        assertEquals(before + 1, reviewRepository.count());
        Review stored = reviewRepository.findByUserIdAndBusinessId(visitor.getId(), kota.getId()).orElseThrow();
        assertEquals(visitor.getId(), stored.getUser().getId());
        assertEquals(kota.getId(), stored.getBusiness().getId());
    }
}
