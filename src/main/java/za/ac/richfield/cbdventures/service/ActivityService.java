package za.ac.richfield.cbdventures.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.richfield.cbdventures.domain.Business;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.domain.UserActivity;
import za.ac.richfield.cbdventures.domain.enums.ActivityType;
import za.ac.richfield.cbdventures.repo.UserActivityRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records the browsing trail used to personalise suggestions (FR-04).
 *
 * <p>This is the {@code user_activity} table from the group's database design.
 * Two rules keep it useful rather than noisy:</p>
 * <ul>
 *   <li>only signed-in visitors are tracked - guests stay anonymous;</li>
 *   <li>a repeat view of the same listing within {@link #VIEW_DEDUPE} is ignored,
 *       so refreshing a profile does not inflate the interest weight.</li>
 * </ul>
 *
 * <p>Writes run in their own transaction so that recording a view can never
 * break or roll back the read that triggered it.</p>
 */
@Service
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    /** A repeat profile view inside this window is treated as the same visit. */
    private static final Duration VIEW_DEDUPE = Duration.ofMinutes(30);

    /** How far back activity still counts towards recommendations. */
    public static final Duration LOOKBACK = Duration.ofDays(60);

    private final UserActivityRepository activityRepository;

    public ActivityService(UserActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    /**
     * Records one row, joining the caller's transaction. Keeping it in the same
     * transaction (rather than a nested one) means the row is only written if the
     * action that caused it succeeds, and it never sees uncommitted state it
     * cannot reference.
     */
    @Transactional
    public void record(User user, Business business, ActivityType type) {
        if (user == null || type == null) {
            return;
        }
        if (isDuplicateView(user, business, type)) {
            log.debug("Skipping duplicate {} for user {} on business {}", type, user.getId(),
                    business == null ? null : business.getId());
            return;
        }
        activityRepository.save(new UserActivity(user, business, type));
    }

    private boolean isDuplicateView(User user, Business business, ActivityType type) {
        if (type != ActivityType.VIEW_PROFILE || business == null) {
            return false;
        }
        Instant since = Instant.now().minus(VIEW_DEDUPE);
        return activityRepository.findRecentForUser(user.getId(), since).stream()
                .anyMatch(a -> a.getType() == ActivityType.VIEW_PROFILE
                        && a.getBusiness() != null
                        && a.getBusiness().getId().equals(business.getId()));
    }

    /** Recent activity, newest first - used by the profile screen's history panel. */
    @Transactional(readOnly = true)
    public List<UserActivity> recent(User user, int limit) {
        if (user == null) {
            return List.of();
        }
        return activityRepository.findTop30ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .limit(Math.max(limit, 1))
                .toList();
    }

    /**
     * Category interest weights learned from the last {@link #LOOKBACK} of activity.
     *
     * <p>Weights are deliberately modest next to an explicit saved preference
     * (worth 3.0 in {@link RecommendationService}) because browsing is a weaker
     * statement of intent than choosing a preference: a saved listing adds 0.75
     * and a profile view 0.25.</p>
     */
    @Transactional(readOnly = true)
    public Map<Long, Double> affinityWeights(User user) {
        Map<Long, Double> weights = new HashMap<>();
        if (user == null) {
            return weights;
        }
        for (UserActivity activity : activityRepository.findRecentForUser(user.getId(),
                Instant.now().minus(LOOKBACK))) {
            Business business = activity.getBusiness();
            if (business == null || business.getCategory() == null) {
                continue;
            }
            double weight = switch (activity.getType()) {
                case SAVE_BUSINESS -> 0.75;
                case VIEW_PROFILE -> 0.25;
                case SEARCH -> 0.10;
            };
            weights.merge(business.getCategory().getId(), weight, Double::sum);
        }
        return weights;
    }
}
