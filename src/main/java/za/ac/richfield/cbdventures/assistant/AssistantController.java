package za.ac.richfield.cbdventures.assistant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public endpoints behind the in-app "Assistant" tab.
 *
 * <p>Mirrors the API of the standalone CBDVentures Assistant project
 * ({@code GET /api/config}, {@code GET /api/places}, {@code POST /api/chat}) so the
 * integration is a drop-in, while running inside the Spring Boot app: one jar,
 * one port, no second process.</p>
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private static final int MAX_MESSAGES = 30;
    private static final int MAX_MESSAGE_CHARS = 2_000;
    private static final int RATE_LIMIT = 30;
    private static final long RATE_WINDOW_MS = 60_000;

    private final AssistantDirectory directory;
    private final AssistantService assistant;
    private final Map<String, long[]> rateWindow = new ConcurrentHashMap<>();

    public AssistantController(AssistantDirectory directory, AssistantService assistant) {
        this.directory = directory;
        this.assistant = assistant;
    }

    /* ---------------------------------------------------------------- DTOs */

    public record CategoryDto(String id, String label, String icon) { }

    public record ConfigDto(String name, double campusLat, double campusLng, String campusAddress,
                            int radiusMetres, List<CategoryDto> categories, int placeCount,
                            int nearbyCount) { }

    public record PlaceDto(String id, String name, String category, String categoryLabel, String address,
                           String description, List<String> highlights, String image, Integer distanceMeters,
                           boolean withinRadius, String verificationNote, String directionsUrl,
                           String mapSearchUrl) { }

    public record ChatRequest(List<AssistantService.Message> messages) { }

    public record ChatResponse(String reply, String mode, String warning, List<PlaceDto> places,
                               List<String> quickReplies) { }

    /* ----------------------------------------------------------- endpoints */

    @GetMapping("/config")
    public ConfigDto config() {
        return new ConfigDto("CBD Ventures AI Assistance",
                AssistantDirectory.CAMPUS_LAT, AssistantDirectory.CAMPUS_LNG,
                AssistantDirectory.CAMPUS_ADDRESS,
                AssistantDirectory.RADIUS_METRES,
                AssistantDirectory.CATEGORIES.stream()
                        .map(category -> new CategoryDto(category.id(), category.label(), category.iconKey()))
                        .toList(),
                directory.everything().size(), directory.nearby().size());
    }

    @GetMapping("/places")
    public List<PlaceDto> places(@RequestParam(required = false) String category,
                                 @RequestParam(required = false) String q) {
        List<AssistantPlace> source;
        if (category != null && !category.isBlank()) {
            source = directory.byCategory(category.trim());
        } else if (q != null && !q.isBlank()) {
            String needle = AssistantDirectory.normaliseWords(q);
            source = directory.everything().stream()
                    .filter(place -> AssistantDirectory.normaliseWords(
                            place.name() + " " + place.address() + " " + String.join(" ", place.highlights()))
                            .contains(needle))
                    .toList();
        } else {
            List<AssistantPlace> combined = new ArrayList<>(directory.nearby());
            combined.addAll(directory.pending());
            source = combined;
        }
        return source.stream().map(AssistantController::toDto).toList();
    }

    @GetMapping("/places/{id}")
    public ResponseEntity<PlaceDto> place(@org.springframework.web.bind.annotation.PathVariable String id) {
        return directory.everything().stream()
                .filter(place -> place.id().equals(id))
                .findFirst()
                .map(place -> ResponseEntity.ok(toDto(place)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request, HttpServletRequest http,
                            Authentication authentication) {
        List<AssistantService.Message> messages = request == null || request.messages() == null
                ? List.of() : request.messages();
        if (messages.isEmpty()) throw new AssistantBadRequest("Send at least one message.");
        if (messages.size() > MAX_MESSAGES) {
            throw new AssistantBadRequest("Please keep the conversation to " + MAX_MESSAGES + " messages or fewer.");
        }
        AssistantService.Message last = messages.get(messages.size() - 1);
        if (last == null || !"user".equals(last.role())) {
            throw new AssistantBadRequest("The last message must come from you.");
        }
        String question = last.content() == null ? "" : last.content().trim();
        if (question.isEmpty()) throw new AssistantBadRequest("Ask me something about places around campus.");
        if (question.length() > MAX_MESSAGE_CHARS) {
            throw new AssistantBadRequest("That message is too long — keep it under " + MAX_MESSAGE_CHARS + " characters.");
        }
        enforceRateLimit(http);

        AssistantService.Reply reply = assistant.respond(question, messages, authentication == null
                ? null : authentication.getName());

        List<PlaceDto> cards = reply.suggestedPlaceIds().stream()
                .map(id -> directory.everything().stream()
                        .filter(place -> place.id().equals(id)).findFirst().orElse(null))
                .filter(place -> place != null)
                .map(AssistantController::toDto)
                .toList();

        return new ChatResponse(reply.reply(), reply.mode(), reply.warning(), cards,
                List.of("Food near campus", "Somewhere to study", "Check my budget", "Directions from Richfield"));
    }

    /* ------------------------------------------------------------- helpers */

    static PlaceDto toDto(AssistantPlace place) {
        return new PlaceDto(place.id(), place.name(), place.category(),
                AssistantDirectory.labelFor(place.category()), place.address(), place.description(),
                place.highlights(), place.image(), place.distanceMeters(), place.withinRadius(),
                place.verificationNote(), place.directionsUrl(), place.mapSearchUrl());
    }

    private void enforceRateLimit(HttpServletRequest request) {
        String key = request.getRemoteAddr() == null ? "local" : request.getRemoteAddr();
        long now = System.currentTimeMillis();
        long[] window = rateWindow.computeIfAbsent(key, ignored -> new long[]{now, 0});
        synchronized (window) {
            if (now - window[0] > RATE_WINDOW_MS) {
                window[0] = now;
                window[1] = 0;
            }
            if (++window[1] > RATE_LIMIT) {
                throw new AssistantRateLimited();
            }
        }
        if (rateWindow.size() > 5_000) rateWindow.clear();
    }

    /* --------------------------------------------------------------- errors */

    static class AssistantBadRequest extends RuntimeException {
        AssistantBadRequest(String message) { super(message); }
    }

    static class AssistantRateLimited extends RuntimeException {
        AssistantRateLimited() { super("Too many requests"); }
    }

    record ErrorDto(String message) { }

    @ExceptionHandler(AssistantBadRequest.class)
    public ResponseEntity<ErrorDto> badRequest(AssistantBadRequest failure) {
        return ResponseEntity.badRequest().body(new ErrorDto(failure.getMessage()));
    }

    @ExceptionHandler(AssistantRateLimited.class)
    public ResponseEntity<ErrorDto> rateLimited(AssistantRateLimited failure) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorDto("You’re asking a lot at once — give me a few seconds and try again."));
    }
}
