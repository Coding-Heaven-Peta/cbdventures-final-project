package za.ac.richfield.cbdventures.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import za.ac.richfield.cbdventures.domain.Amenity;
import za.ac.richfield.cbdventures.domain.Business;
import za.ac.richfield.cbdventures.domain.BusinessImage;
import za.ac.richfield.cbdventures.domain.Category;
import za.ac.richfield.cbdventures.domain.EventListing;
import za.ac.richfield.cbdventures.domain.Review;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.domain.enums.PriceLevel;
import za.ac.richfield.cbdventures.domain.enums.Role;
import za.ac.richfield.cbdventures.error.ApiException;
import za.ac.richfield.cbdventures.repo.AmenityRepository;
import za.ac.richfield.cbdventures.repo.BusinessRepository;
import za.ac.richfield.cbdventures.repo.CategoryRepository;
import za.ac.richfield.cbdventures.repo.EventRepository;
import za.ac.richfield.cbdventures.repo.ReviewRepository;
import za.ac.richfield.cbdventures.repo.UserRepository;
import za.ac.richfield.cbdventures.web.dto.BackupDtos;
import za.ac.richfield.cbdventures.web.dto.ReportDtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 4 tests for the reporting deliverables: the five management reports
 * and the system backup/restore round trip.
 *
 * <p>The {@code test} profile starts with an empty database, so each test builds
 * its own fixture and rolls back afterwards.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Management reports and system backup")
class ReportingTest {

    /** A real BCrypt-shaped hash so the backup can be checked without encoding. */
    private static final String HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Autowired ReportService reportService;
    @Autowired BackupService backupService;
    @Autowired CategoryRepository categoryRepository;
    @Autowired AmenityRepository amenityRepository;
    @Autowired BusinessRepository businessRepository;
    @Autowired ReviewRepository reviewRepository;
    @Autowired UserRepository userRepository;
    @Autowired EventRepository eventRepository;
    @Autowired tools.jackson.databind.ObjectMapper objectMapper;

    private User admin;
    private User owner;
    private User visitor;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(new User("Test Administrator", "admin@test.co.za", HASH, Role.ADMIN));
        owner = userRepository.save(new User("Test Owner", "owner@test.co.za", HASH, Role.OWNER));
        visitor = userRepository.save(new User("Test Student", "student@test.co.za", HASH, Role.VISITOR));

        Category food = categoryRepository.save(
                new Category("Food & Drinks", "food-drinks", "Quick eats", "food", 1));
        Category salons = categoryRepository.save(
                new Category("Salons & Personal Care", "salons", "Hair and nails", "salon", 2));

        Amenity wiFi = amenityRepository.save(new Amenity(food, "Free Wi-Fi", "wifi"));

        Business kata = business("Kota Kingdom", food, BusinessStatus.ACTIVE, "-25.746000", "28.192800");
        kata.getAmenities().add(wiFi);
        kata.addImage(new BusinessImage("/images/covers/food-drinks-01.svg", "Front", true));
        businessRepository.save(kata);

        Business braai = business("Braam's Braai Corner", food, BusinessStatus.ACTIVE,
                "-25.746500", "28.192300");
        businessRepository.save(braai);

        business("Bloom Nail Studio", salons, BusinessStatus.PENDING, "-25.747000", "28.193000");
        business("Quiet Room Study Hub", food, BusinessStatus.REJECTED, "-25.746100", "28.192600");

        Business kota = businessRepository.findAll().stream()
                .filter(b -> b.getName().equals("Kota Kingdom")).findFirst().orElseThrow();

        reviewRepository.save(new Review(visitor, kota, 5, "Best kotas near campus."));
        reviewRepository.save(new Review(admin, kota, 4, "Good value, gets busy."));

        EventListing event = new EventListing();
        event.setBusiness(kota);
        event.setTitle("Friday Night Market");
        event.setDescription("Street food and music.");
        event.setVenue("Church Square");
        event.setEventDateTime(LocalDateTime.now().plusDays(5));
        event.setTicketPrice(BigDecimal.ZERO);
        eventRepository.save(event);
    }

    private Business business(String name, Category category, BusinessStatus status,
                              String lat, String lng) {
        Business business = new Business();
        business.setOwner(owner);
        business.setCategory(category);
        business.setName(name);
        business.setAddress("1 Test Street, Pretoria Central");
        business.setDescription("Test listing for the reporting suite.");
        business.setLatitude(new BigDecimal(lat));
        business.setLongitude(new BigDecimal(lng));
        business.setPriceLevel(PriceLevel.R);
        business.setOperatingHours("Mon-Fri 08:00 - 17:00");
        business.setStatus(status);
        business.setAdminNote(status == BusinessStatus.REJECTED
                ? "Please add a contact number." : "Verified by the CBDVentures team.");
        return businessRepository.save(business);
    }

    /* ------------------------------------------------------------- reports */

    @Test
    @DisplayName("the catalogue advertises five reports (sheet asks for 3-5)")
    void catalogueHasFiveReports() {
        List<ReportDtos.ReportInfo> catalogue = reportService.catalogue();
        assertThat(catalogue).hasSize(5);
        assertThat(catalogue).extracting(ReportDtos.ReportInfo::key)
                .containsExactly("listings-by-category", "verification-audit", "review-activity",
                        "owner-performance", "events-schedule");
    }

    @Test
    @DisplayName("every report has columns, data rows and headline metrics")
    void everyReportIsPopulated() {
        for (ReportDtos.ReportInfo info : reportService.catalogue()) {
            ReportDtos.ReportView report = reportService.run(info.key());
            assertThat(report.columns()).as("%s columns", info.key()).isNotEmpty();
            assertThat(report.rows()).as("%s rows", info.key()).isNotEmpty();
            assertThat(report.metrics()).as("%s metrics", info.key()).hasSize(4);
            report.rows().forEach(row -> assertThat(row)
                    .as("%s row width matches the header", info.key())
                    .hasSize(report.columns().size()));
        }
    }

    @Test
    @DisplayName("the listings report totals match the database")
    void listingsReportTotalsAreCorrect() {
        ReportDtos.ReportView report = reportService.run("listings-by-category");
        List<String> total = report.rows().get(report.rows().size() - 1);

        assertThat(total.get(0)).isEqualTo("TOTAL");
        assertThat(Long.parseLong(total.get(4))).isEqualTo(businessRepository.count());
        assertThat(Long.parseLong(total.get(1))).isEqualTo(
                businessRepository.countByStatus(BusinessStatus.ACTIVE));
    }

    @Test
    @DisplayName("the verification report lists pending submissions first and shows the note")
    void verificationReportStartsWithPendingItems() {
        ReportDtos.ReportView report = reportService.run("verification-audit");

        assertThat(report.rows().get(0).get(3)).isEqualTo("Awaiting review");
        assertThat(report.rows()).anySatisfy(row -> {
            assertThat(row.get(3)).isEqualTo("Rejected");
            assertThat(row.get(4)).isEqualTo("Please add a contact number.");
        });
    }

    @Test
    @DisplayName("the review report counts every review on a live listing")
    void reviewReportCoversAllReviews() {
        ReportDtos.ReportView report = reportService.run("review-activity");
        long reported = report.metrics().stream()
                .filter(metric -> metric.label().equals("Reviews"))
                .map(metric -> Long.parseLong(metric.value()))
                .findFirst()
                .orElse(0L);

        assertThat(reported).isEqualTo(reviewRepository.count());
        assertThat(report.rows()).anySatisfy(row -> {
            assertThat(row.get(0)).isEqualTo("Braam's Braai Corner");
            assertThat(row.get(2)).isEqualTo("0");
        });
    }

    @Test
    @DisplayName("the event report renders a free ticket as 'Free'")
    void eventsReportFormatsPrices() {
        ReportDtos.ReportView report = reportService.run("events-schedule");
        List<String> row = report.rows().get(0);

        assertThat(row.get(0)).isEqualTo("Friday Night Market");
        assertThat(row.get(4)).isEqualTo("Free");
    }

    @Test
    @DisplayName("an unknown report key is a 404, not a crash")
    void unknownReportIsNotFound() {
        assertThatThrownBy(() -> reportService.run("does-not-exist"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    @DisplayName("CSV export quotes every cell so commas in notes stay safe")
    void csvIsQuotedAndComplete() {
        ReportDtos.ReportView report = reportService.run("listings-by-category");
        String csv = reportService.toCsv(report);
        String[] lines = csv.split("\r\n");

        assertThat(lines).hasSize(report.rows().size() + 1);
        assertThat(lines[0]).startsWith("\"Category\",\"Live\"");
        assertThat(csv).contains("\"Food & Drinks\"");
    }

    /* -------------------------------------------------------------- backup */

    @Test
    @DisplayName("a backup contains every table and preserves password hashes")
    void backupExportsEveryTable() {
        BackupDtos.BackupFile backup = backupService.export(admin);

        assertThat(backup.format()).isEqualTo("cbdventures-backup");
        assertThat(backup.version()).isEqualTo(BackupService.currentVersion());
        assertThat(backup.categories()).hasSize(2);
        assertThat(backup.amenities()).hasSize(1);
        assertThat(backup.businesses()).hasSize((int) businessRepository.count());
        assertThat(backup.reviews()).hasSize(2);
        assertThat(backup.events()).hasSize(1);
        assertThat(backup.users()).hasSize(3);
        assertThat(backup.users()).allSatisfy(user -> assertThat(user.passwordHash()).startsWith("$2"));
        assertThat(backup.businesses()).anySatisfy(row ->
                assertThat(row.amenityNames()).containsExactly("Free Wi-Fi"));
        assertThat(backup.counts().businesses()).isEqualTo(backup.businesses().size());
    }

    @Test
    @DisplayName("only an administrator may export or restore")
    void backupRequiresAdmin() {
        assertThatThrownBy(() -> backupService.export(owner))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("administrators");
        assertThatThrownBy(() -> backupService.restore(visitor, "{}"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("administrators");
        assertThatThrownBy(() -> backupService.export(null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("a non-backup file is rejected without touching the data")
    void invalidBackupIsRejected() {
        long before = businessRepository.count();

        assertThatThrownBy(() -> backupService.restore(admin, "{\"hello\":\"world\"}"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not a valid CBDVentures backup");

        assertThat(businessRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("backup then restore reproduces the same database")
    void backupRestoreRoundTrip() {
        long businessesBefore = businessRepository.count();
        long reviewsBefore = reviewRepository.count();
        long usersBefore = userRepository.count();
        long eventsBefore = eventRepository.count();

        String json = objectMapper.writeValueAsString(backupService.export(admin));
        BackupDtos.RestoreResult result = backupService.restore(admin, json);

        assertThat(result.counts().businesses()).isEqualTo((int) businessesBefore);
        assertThat(businessRepository.count()).isEqualTo(businessesBefore);
        assertThat(reviewRepository.count()).isEqualTo(reviewsBefore);
        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(eventRepository.count()).isEqualTo(eventsBefore);
        assertThat(businessRepository.findAll())
                .extracting(Business::getName)
                .contains("Kota Kingdom", "Bloom Nail Studio", "Quiet Room Study Hub");
        // hashes survived the round trip, so accounts can still sign in
        assertThat(userRepository.findByEmailIgnoreCase("admin@test.co.za"))
                .get()
                .extracting(User::getPasswordHash)
                .asString()
                .startsWith("$2");
    }
}
