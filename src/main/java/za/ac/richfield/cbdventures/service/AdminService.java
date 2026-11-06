package za.ac.richfield.cbdventures.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.richfield.cbdventures.domain.Business;
import za.ac.richfield.cbdventures.domain.Category;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.domain.enums.Role;
import za.ac.richfield.cbdventures.error.ApiException;
import za.ac.richfield.cbdventures.repo.*;
import za.ac.richfield.cbdventures.web.dto.AdminDtos;
import za.ac.richfield.cbdventures.web.dto.BusinessCard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Administrator functions: verify newly submitted listings (Phase 3
 * "verification before publishing" rule) and report platform statistics.
 */
@Service
public class AdminService {

    private final BusinessRepository businessRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final BusinessMapper mapper;
    private final ZoneService zoneService;

    public AdminService(BusinessRepository businessRepository,
                        ReviewRepository reviewRepository,
                        UserRepository userRepository,
                        CategoryRepository categoryRepository,
                        EventRepository eventRepository,
                        BusinessMapper mapper,
                        ZoneService zoneService) {
        this.businessRepository = businessRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.mapper = mapper;
        this.zoneService = zoneService;
    }

    @Transactional(readOnly = true)
    public List<BusinessCard> byStatus(BusinessStatus status) {
        List<Business> businesses = businessRepository.findByStatusOrderByCreatedAtDesc(status);
        Map<Long, BusinessMapper.Rating> ratings = mapper.ratingsFor(businesses);
        return businesses.stream()
                .map(b -> mapper.toCard(b, ratings.get(b.getId()),
                        zoneService.anchorLatitude(), zoneService.anchorLongitude()))
                .toList();
    }

    @Transactional
    public BusinessCard decide(User admin, Long businessId, BusinessStatus decision, String note) {
        if (admin.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Only administrators can verify listings.");
        }
        if (decision != BusinessStatus.ACTIVE && decision != BusinessStatus.REJECTED
                && decision != BusinessStatus.INACTIVE) {
            throw ApiException.badRequest("Unsupported verification decision.");
        }
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> ApiException.notFound("That business could not be found."));
        business.setStatus(decision);
        business.setAdminNote(note == null || note.isBlank()
                ? (decision == BusinessStatus.ACTIVE
                    ? "Verified by the CBDVentures team."
                    : "Not approved. Please update your details and resubmit.")
                : note.trim());
        Business saved = businessRepository.save(business);
        Map<Long, BusinessMapper.Rating> ratings = mapper.ratingsFor(List.of(saved));
        return mapper.toCard(saved, ratings.get(saved.getId()),
                zoneService.anchorLatitude(), zoneService.anchorLongitude());
    }

    @Transactional(readOnly = true)
    public AdminDtos.StatsView stats() {
        List<Business> all = businessRepository.findAll();
        long reviews = reviewRepository.count();
        double average = all.stream()
                .map(b -> mapper.ratingsFor(List.of(b)).getOrDefault(b.getId(), BusinessMapper.Rating.EMPTY))
                .filter(rating -> rating.count() > 0)
                .mapToDouble(BusinessMapper.Rating::average)
                .average()
                .orElse(0.0);

        List<AdminDtos.CategoryCount> byCategory = categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(category -> new AdminDtos.CategoryCount(category.getName(), category.getSlug(),
                        all.stream().filter(b -> b.getCategory().getId().equals(category.getId())).count()))
                .toList();

        List<User> users = userRepository.findAll();
        return new AdminDtos.StatsView(
                all.size(),
                all.stream().filter(b -> b.getStatus() == BusinessStatus.ACTIVE).count(),
                all.stream().filter(b -> b.getStatus() == BusinessStatus.PENDING).count(),
                reviews,
                users.size(),
                users.stream().filter(u -> u.getRole() == Role.OWNER).count(),
                eventRepository.count(),
                Math.round(average * 10.0) / 10.0,
                byCategory);
    }

    /** Simple breakdown used by the admin dashboard header. */
    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        AdminDtos.StatsView stats = stats();
        summary.put("businesses", stats.totalBusinesses());
        summary.put("active", stats.activeBusinesses());
        summary.put("pending", stats.pendingBusinesses());
        summary.put("reviews", stats.reviews());
        summary.put("users", stats.users());
        summary.put("events", stats.events());
        summary.put("averageRating", stats.averageRating());
        return summary;
    }

    @Transactional(readOnly = true)
    public List<Category> categories() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }
}
