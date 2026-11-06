package za.ac.richfield.cbdventures.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.security.CurrentUser;
import za.ac.richfield.cbdventures.service.BusinessQuery;
import za.ac.richfield.cbdventures.service.BusinessService;
import za.ac.richfield.cbdventures.service.ReviewService;
import za.ac.richfield.cbdventures.web.dto.BusinessCard;
import za.ac.richfield.cbdventures.web.dto.MapPin;
import za.ac.richfield.cbdventures.web.dto.BusinessDetail;
import za.ac.richfield.cbdventures.web.dto.PageResponse;
import za.ac.richfield.cbdventures.web.dto.ReviewDtos;

import java.util.List;

/**
 * Public discovery endpoints (use cases: search, browse by category, filter,
 * view profile, view reviews, leave a review).
 */
@RestController
@RequestMapping("/api/businesses")
public class BusinessController {

    private final BusinessService businessService;
    private final ReviewService reviewService;

    public BusinessController(BusinessService businessService, ReviewService reviewService) {
        this.businessService = businessService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public PageResponse<BusinessCard> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(name = "price", required = false) List<Long> price,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxDistanceKm,
            @RequestParam(required = false) List<Long> amenity,
            @RequestParam(required = false) Boolean openNow,
            @RequestParam(required = false, defaultValue = "recommended") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "24") int size,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {

        BusinessQuery query = new BusinessQuery(q, category, price, minRating, maxDistanceKm,
                amenity, openNow, sort, page, size, lat, lng);
        return businessService.search(query);
    }

    /** Coordinates for the zone map (declared before /{id} so "map" is not parsed as an id). */
    @GetMapping("/map")
    public List<MapPin> map() {
        return businessService.mapPins();
    }

    @GetMapping("/{id}")
    public BusinessDetail detail(@PathVariable Long id) {
        return businessService.detail(id, CurrentUser.optional().orElse(null));
    }

    @GetMapping("/{id}/reviews")
    public List<ReviewDtos.ReviewView> reviews(@PathVariable Long id) {
        return reviewService.listForBusiness(id, CurrentUser.optional().orElse(null));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewDtos.ReviewView> review(@PathVariable Long id,
                                                        @Valid @RequestBody ReviewDtos.ReviewRequest request) {
        User user = CurrentUser.require();
        reviewService.upsert(user, id, request);
        List<ReviewDtos.ReviewView> reviews = reviewService.listForBusiness(id, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviews.stream()
                .filter(ReviewDtos.ReviewView::mine)
                .findFirst()
                .orElse(null));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.delete(CurrentUser.require(), reviewId);
        return ResponseEntity.noContent().build();
    }
}
