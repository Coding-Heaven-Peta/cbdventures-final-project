package za.ac.richfield.cbdventures.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import za.ac.richfield.cbdventures.domain.*;
import za.ac.richfield.cbdventures.domain.enums.ActivityType;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.domain.enums.PriceLevel;
import za.ac.richfield.cbdventures.domain.enums.Role;
import za.ac.richfield.cbdventures.error.ApiException;
import za.ac.richfield.cbdventures.repo.BusinessHoursRepository;
import za.ac.richfield.cbdventures.repo.BusinessRepository;
import za.ac.richfield.cbdventures.repo.CategoryRepository;
import za.ac.richfield.cbdventures.repo.UserActivityRepository;
import za.ac.richfield.cbdventures.repo.UserRepository;
import za.ac.richfield.cbdventures.web.dto.BusinessCard;
import za.ac.richfield.cbdventures.web.dto.BusinessDetail;
import za.ac.richfield.cbdventures.web.dto.BusinessRequest;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4 — tests for the per-day opening hours and the activity trail, the two
 * pieces merged in from the group's database design.
 *
 * <p>Unit tests cover the {@link BusinessHours} rules in isolation; the
 * integration tests prove the rules survive the round trip through the service,
 * the mapper and the database.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OpeningHoursTest {

    @Autowired CategoryRepository categoryRepository;
    @Autowired BusinessRepository businessRepository;
    @Autowired BusinessHoursRepository hoursRepository;
    @Autowired UserActivityRepository activityRepository;
    @Autowired UserRepository userRepository;
    @Autowired BusinessService businessService;
    @Autowired BusinessMapper mapper;
    @Autowired ActivityService activityService;
    @Autowired RecommendationService recommendationService;

    private Category food;
    private User owner;
    private User visitor;

    @BeforeEach
    void setUp() {
        food = categoryRepository.save(new Category("Food & Drinks", "food-drinks",
                "Quick eats", "food", 1));
        owner = userRepository.save(new User("Hours Owner", "hours.owner@test.co.za", "{bcrypt}", Role.OWNER));
        visitor = userRepository.save(new User("Hours Visitor", "hours.visitor@test.co.za", "{bcrypt}", Role.VISITOR));
    }

    /* ------------------------------------------------------- unit: the entity */

    @Test
    @DisplayName("A closed day reports itself closed and is never open")
    void closedDayIsNeverOpen() {
        BusinessHours sunday = BusinessHours.closed(DayOfWeek.SUNDAY);
        assertTrue(sunday.isClosed());
        assertFalse(sunday.covers(LocalTime.NOON));
        assertTrue(sunday.isFor(DayOfWeek.SUNDAY));
        assertFalse(sunday.isFor(DayOfWeek.MONDAY));
    }

    @Test
    @DisplayName("A normal window only covers times strictly inside it")
    void normalWindow() {
        BusinessHours monday = BusinessHours.open(DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(17, 0));
        assertTrue(monday.covers(LocalTime.of(8, 0)), "opening time counts as open");
        assertTrue(monday.covers(LocalTime.NOON));
        assertFalse(monday.covers(LocalTime.of(17, 0)), "closing time counts as closed");
        assertFalse(monday.covers(LocalTime.of(7, 59)));
    }

    @Test
    @DisplayName("A window that crosses midnight covers both sides of midnight")
    void midnightWindow() {
        BusinessHours friday = BusinessHours.open(DayOfWeek.FRIDAY,
                LocalTime.of(18, 0), LocalTime.of(2, 0));
        assertTrue(friday.covers(LocalTime.of(23, 30)));
        assertTrue(friday.covers(LocalTime.of(1, 0)));
        assertFalse(friday.covers(LocalTime.NOON));
    }

    /* ---------------------------------------- integration: creating a listing */

    @Test
    @DisplayName("Per-day hours are saved and round-trip through the profile view")
    void hoursRoundTrip() {
        List<BusinessRequest.HoursRequest> week = new ArrayList<>();
        for (int day = 1; day <= 5; day++) {
            week.add(new BusinessRequest.HoursRequest(day, false, "07:00", "17:00"));
        }
        week.add(new BusinessRequest.HoursRequest(6, false, "08:00", "13:00"));
        week.add(new BusinessRequest.HoursRequest(7, true, null, null));

        Long id = businessService.create(owner, request("Kota Corner Hours", week)).id();

        Business saved = businessRepository.findById(id).orElseThrow();
        assertEquals(7, saved.getHours().size());
        assertTrue(saved.hoursFor(DayOfWeek.SUNDAY).isClosed());
        assertEquals(LocalTime.of(8, 0), saved.hoursFor(DayOfWeek.SATURDAY).getOpenTime());

        BusinessDetail detail = businessService.detail(id, owner);
        assertEquals(7, detail.hours().size());
        assertEquals("Mon", detail.hours().getFirst().shortDayName());
        assertTrue(detail.hours().getLast().closed(), "Sunday is a closed day");
        assertTrue(detail.hours().stream().anyMatch(BusinessDetail.HoursView::today),
                "exactly today's row is flagged for the UI");
        assertEquals(1, detail.hours().stream().filter(BusinessDetail.HoursView::today).count());
    }

    @Test
    @DisplayName("An open day with no times is rejected")
    void openDayNeedsTimes() {
        List<BusinessRequest.HoursRequest> week = List.of(
                new BusinessRequest.HoursRequest(1, false, null, null));
        ApiException ex = assertThrows(ApiException.class,
                () -> businessService.create(owner, request("Missing Times", week)));
        assertTrue(ex.getMessage().contains("both an opening and a closing time"));
    }

    @Test
    @DisplayName("A closed day may not carry times")
    void closedDayMayNotCarryTimes() {
        List<BusinessRequest.HoursRequest> week = List.of(
                new BusinessRequest.HoursRequest(3, true, "08:00", "12:00"));
        ApiException ex = assertThrows(ApiException.class,
                () -> businessService.create(owner, request("Confused Sunday", week)));
        assertTrue(ex.getMessage().contains("marked as a closed day"));
    }

    @Test
    @DisplayName("Opening time must be earlier than closing time on every day")
    void openingMustPrecedeClosing() {
        List<BusinessRequest.HoursRequest> week = List.of(
                new BusinessRequest.HoursRequest(2, false, "18:00", "09:00"));
        ApiException ex = assertThrows(ApiException.class,
                () -> businessService.create(owner, request("Backwards Tuesday", week)));
        assertTrue(ex.getMessage().contains("earlier than the closing time"));
    }

    @Test
    @DisplayName("The same weekday cannot appear twice")
    void duplicateDayRejected() {
        List<BusinessRequest.HoursRequest> week = List.of(
                new BusinessRequest.HoursRequest(4, false, "08:00", "12:00"),
                new BusinessRequest.HoursRequest(4, false, "13:00", "17:00"));
        ApiException ex = assertThrows(ApiException.class,
                () -> businessService.create(owner, request("Double Thursday", week)));
        assertTrue(ex.getMessage().contains("twice"));
    }

    @Test
    @DisplayName("A listing with only the legacy single window still answers open-now")
    void legacyFallbackStillWorks() {
        Business legacy = new Business();
        legacy.setName("Legacy Deli");
        legacy.setOwner(owner);
        legacy.setCategory(food);
        legacy.setAddress("2 Church Street");
        legacy.setDescription("Only has a single window.");
        legacy.setLatitude(BigDecimal.valueOf(-25.7465));
        legacy.setLongitude(BigDecimal.valueOf(28.1927));
        legacy.setPriceLevel(PriceLevel.R);
        legacy.setStatus(BusinessStatus.ACTIVE);
        legacy.setOperatingHours("Daily 00:00 - 23:59");
        legacy.setOpenTime(LocalTime.MIN);
        legacy.setCloseTime(LocalTime.of(23, 59));
        businessRepository.save(legacy);

        assertTrue(legacy.getHours().isEmpty());
        assertEquals(Boolean.TRUE, mapper.openNow(legacy), "flat hours are the fallback");
        assertEquals("00:00 - 23:59", mapper.todayHours(legacy));
    }

    @Test
    @DisplayName("A closed day makes the card say so, and the card carries today's window")
    void cardShowsTodaysHours() {
        Long id = businessService.create(owner,
                request("Weekday Only", List.of(
                        new BusinessRequest.HoursRequest(1, false, "07:00", "17:00"),
                        new BusinessRequest.HoursRequest(2, false, "07:00", "17:00"),
                        new BusinessRequest.HoursRequest(3, false, "07:00", "17:00"),
                        new BusinessRequest.HoursRequest(4, false, "07:00", "17:00"),
                        new BusinessRequest.HoursRequest(5, false, "07:00", "17:00"),
                        new BusinessRequest.HoursRequest(6, true, null, null),
                        new BusinessRequest.HoursRequest(7, true, null, null)))).id();

        Business saved = businessRepository.findById(id).orElseThrow();
        BusinessCard card = mapper.toCard(saved, null, null, null);
        assertNotNull(card.todayHours());
        assertTrue(card.todayHours().equals("Closed today")
                        || card.todayHours().equals("07:00 - 17:00"),
                "today's window or a closed notice, never a stale string");
    }

    /* ---------------------------------------- activity trail (FR-04) */

    @Test
    @DisplayName("Opening a profile records a view, and a second view within the window does not")
    void profileViewIsRecordedOnce() {
        Long id = createActive("View Me", List.of());

        businessService.detail(id, visitor);
        businessService.detail(id, visitor);

        List<UserActivity> trail = activityRepository.findTop30ByUserIdOrderByCreatedAtDesc(visitor.getId());
        assertEquals(1, trail.size(), "30-minute de-duplication keeps one row per visit");
        assertEquals(ActivityType.VIEW_PROFILE, trail.getFirst().getType());
    }

    @Test
    @DisplayName("Guests are never tracked")
    void guestsAreNotTracked() {
        Long id = createActive("Public Place", List.of());
        businessService.detail(id, null);
        assertEquals(0, activityRepository.count());
    }

    @Test
    @DisplayName("An owner opening their own listing is not counted as interest")
    void ownerViewsAreNotInterest() {
        Long id = createActive("My Own Shop", List.of());
        businessService.detail(id, owner);
        assertEquals(0, activityRepository.count());
    }

    @Test
    @DisplayName("Browsing a category raises that category's recommendation weight")
    void browsingFeedsRecommendations() {
        Long id = createActive("Browse Me Cafe", List.of());
        businessService.detail(id, visitor);

        Map<Long, Double> weights = activityService.affinityWeights(visitor);
        assertEquals(0.25, weights.get(food.getId()), 0.001,
                "a single profile view is a weak signal");

        // A second listing in the same category strengthens it further.
        businessService.detail(createActive("Browse Me Too", List.of()), visitor);
        assertTrue(activityService.affinityWeights(visitor).get(food.getId()) >= 0.5);
    }

    /* ------------------------------------------------------------ fixtures */

    /** Creates a listing and publishes it, because only ACTIVE listings are viewable. */
    private Long createActive(String name, List<BusinessRequest.HoursRequest> hours) {
        Long id = businessService.create(owner, request(name, hours)).id();
        businessService.setStatus(owner, id, BusinessStatus.ACTIVE, "Approved for the test.");
        return id;
    }

    private BusinessRequest request(String name, List<BusinessRequest.HoursRequest> hours) {
        return new BusinessRequest(name, "Test listing", "1 Test Street, Pretoria Central",
                BigDecimal.valueOf(-25.7464), BigDecimal.valueOf(28.1925),
                PriceLevel.R, food.getId(), null, "012 000 0000", null, null,
                null, null, hours, List.of(), List.of(), true);
    }
}
