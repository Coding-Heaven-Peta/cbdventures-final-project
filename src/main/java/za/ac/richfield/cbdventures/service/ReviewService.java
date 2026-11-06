package za.ac.richfield.cbdventures.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.richfield.cbdventures.domain.Business;
import za.ac.richfield.cbdventures.domain.Review;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.domain.enums.ActivityType;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.domain.enums.Role;
import za.ac.richfield.cbdventures.error.ApiException;
import za.ac.richfield.cbdventures.repo.BusinessRepository;
import za.ac.richfield.cbdventures.repo.ReviewRepository;
import za.ac.richfield.cbdventures.web.dto.ReviewDtos;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Review rules from Phase 3:
 * <ul>
 *   <li>rating is a mandatory whole number from 1 to 5, comment optional;</li>
 *   <li>a review must belong to a real user and an active listing;</li>
 *   <li>one active review per user per business (a resubmission updates it);</li>
 *   <li>the same business cannot be reviewed twice within 24 hours (anti-spam);</li>
 *   <li>an owner may not review a business they manage.</li>
 * </ul>
 * The average rating is always recalculated from stored reviews, never cached.
 */
@Service
public class ReviewService {

    private static final Duration REVIEW_COOLDOWN = Duration.ofHours(24);

    private final ReviewRepository reviewRepository;
    private final BusinessRepository businessRepository;
    private final BusinessMapper mapper;
    private final ActivityService activityService;

    public ReviewService(ReviewRepository reviewRepository,
                         BusinessRepository businessRepository,
                         BusinessMapper mapper,
                         ActivityService activityService) {
        this.reviewRepository = reviewRepository;
        this.businessRepository = businessRepository;
        this.mapper = mapper;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public List<ReviewDtos.ReviewView> listForBusiness(Long businessId, User viewer) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> ApiException.notFound("That business could not be found."));
        boolean privileged = viewer != null && (viewer.getRole() == Role.ADMIN
                || business.getOwner().getId().equals(viewer.getId()));
        if (business.getStatus() != BusinessStatus.ACTIVE && !privileged) {
            throw ApiException.notFound("That business could not be found.");
        }
        return reviewRepository.findByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .map(review -> mapper.toReviewView(review, viewer))
                .toList();
    }

    @Transactional
    public BusinessMapper.Rating upsert(User user, Long businessId, ReviewDtos.ReviewRequest request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> ApiException.notFound("That business could not be found."));

        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw ApiException.badRequest("Reviews open once this listing is live.");
        }
        if (business.getOwner().getId().equals(user.getId())) {
            throw ApiException.forbidden("You cannot review a business you manage.");
        }
        if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw ApiException.badRequest("Please choose a star rating between 1 and 5.");
        }

        String comment = request.comment() == null ? null : request.comment().trim();
        if (comment != null && comment.isEmpty()) {
            comment = null;
        }

        Review review = reviewRepository.findByUserIdAndBusinessId(user.getId(), businessId).orElse(null);
        Instant now = Instant.now();

        if (review == null) {
            review = new Review(user, business, request.rating(), comment);
            review.setVerified(false);
        } else {
            Duration sinceLastChange = Duration.between(review.getUpdatedAt(), now);
            if (sinceLastChange.compareTo(REVIEW_COOLDOWN) < 0) {
                long hoursLeft = REVIEW_COOLDOWN.minus(sinceLastChange).toHours() + 1;
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "You can update your review once every 24 hours. Please try again in "
                                + hoursLeft + " hour" + (hoursLeft == 1 ? "" : "s") + ".");
            }
            review.setRating(request.rating());
            review.setComment(comment);
            review.setUpdatedAt(now);
        }

        reviewRepository.save(review);
        // Reviewing implies interest: strongest browsing signal short of saving.
        activityService.record(user, business, ActivityType.SAVE_BUSINESS);
        return new BusinessMapper.Rating(averageFor(businessId), countFor(businessId));
    }

    @Transactional
    public void delete(User actor, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("That review could not be found."));
        boolean isAuthor = review.getUser().getId().equals(actor.getId());
        if (!isAuthor && actor.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("You can only remove your own review.");
        }
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public double averageFor(Long businessId) {
        List<Review> reviews = reviewRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);
        return reviews.isEmpty() ? 0.0 : Math.round(reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0) * 10.0) / 10.0;
    }

    @Transactional(readOnly = true)
    public long countFor(Long businessId) {
        return reviewRepository.countByBusinessId(businessId);
    }
}
