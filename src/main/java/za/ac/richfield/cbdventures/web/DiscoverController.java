package za.ac.richfield.cbdventures.web;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.security.CurrentUser;
import za.ac.richfield.cbdventures.service.EventService;
import za.ac.richfield.cbdventures.service.RecommendationService;
import za.ac.richfield.cbdventures.web.dto.*;

import java.util.List;

/**
 * Home screen, personalised suggestions (FR-04) and the events calendar.
 */
@RestController
@RequestMapping("/api")
public class DiscoverController {

    private final RecommendationService recommendationService;
    private final EventService eventService;

    public DiscoverController(RecommendationService recommendationService, EventService eventService) {
        this.recommendationService = recommendationService;
        this.eventService = eventService;
    }

    @GetMapping("/discover/home")
    public HomeResponse home(@RequestParam(required = false) Double lat,
                             @RequestParam(required = false) Double lng) {
        return recommendationService.home(CurrentUser.optional().orElse(null), lat, lng);
    }

    @GetMapping("/recommendations")
    public List<BusinessCard> recommendations(@RequestParam(required = false) Double lat,
                                              @RequestParam(required = false) Double lng,
                                              @RequestParam(required = false) Integer limit) {
        return recommendationService.recommendations(CurrentUser.optional().orElse(null), lat, lng, limit);
    }

    @GetMapping("/events")
    public List<EventDtos.EventView> events(@RequestParam(required = false) String category,
                                            @RequestParam(required = false) Double maxDistanceKm,
                                            @RequestParam(required = false) Integer limit) {
        return eventService.upcoming(category, maxDistanceKm, limit);
    }

    @GetMapping("/preferences")
    public AdminDtos.PreferenceView preferences() {
        User user = CurrentUser.require();
        return recommendationService.preferences(user);
    }

    @PutMapping("/preferences")
    public AdminDtos.PreferenceView savePreferences(@Valid @RequestBody AdminDtos.PreferenceRequest request) {
        User user = CurrentUser.require();
        return recommendationService.savePreferences(user, request);
    }
}
