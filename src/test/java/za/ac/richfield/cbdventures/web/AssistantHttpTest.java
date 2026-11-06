package za.ac.richfield.cbdventures.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4 — the student assistant that shipped with the handover project, now
 * served from this Spring Boot app under {@code /api/assistant}.
 *
 * <p>These tests protect the integration: the tab must be reachable without a
 * token, the directory must keep the 1 km campus rule, and the chat endpoint
 * must answer in the app's own JSON shape.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AssistantHttpTest {

    @LocalServerPort
    int port;

    private final ObjectMapper mapper = new ObjectMapper();

    private RestClient client() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    private JsonNode json(String body) {
        return mapper.readTree(body);
    }

    /** Jackson 3 omits null fields, so "not present" and "null" both mean unset. */
    private boolean absent(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull();
    }

    /* ------------------------------------------------------------- config */

    @Test
    @DisplayName("Assistant config is public and advertises the campus zone, radius and categories")
    void configIsPublic() {
        String body = client().get().uri("/api/assistant/config")
                .retrieve().body(String.class);
        JsonNode node = json(body);

        assertEquals(-25.746467, node.get("campusLat").asDouble(), 1e-6);
        assertEquals(28.19252, node.get("campusLng").asDouble(), 1e-6);
        assertEquals(1000, node.get("radiusMetres").asInt());
        assertEquals(9, node.get("categories").size(),
                "the guide still understands the nine student categories");
        assertTrue(node.get("placeCount").asInt() >= 19, "curated directory is loaded from the classpath");
        assertTrue(node.get("nearbyCount").asInt() >= 1, "at least one place is inside the campus radius");
    }

    /* ------------------------------------------------------------ places */

    @Test
    @DisplayName("Places endpoint filters by category and never reports an out-of-radius distance")
    void placesRespectTheRadius() {
        JsonNode places = json(client().get().uri("/api/assistant/places")
                .retrieve().body(String.class));

        assertTrue(places.size() >= 19);
        assertFalse(places.isEmpty());
        for (JsonNode place : places) {
            assertNotNull(place.get("name").asText());
            assertFalse(place.get("name").asText().isBlank());
            if (place.get("withinRadius").asBoolean()) {
                assertTrue(place.get("distanceMeters").asInt() <= 1000,
                        place.get("name").asText() + " must be inside the 1 km campus radius");
            } else {
                assertTrue(absent(place, "distanceMeters"),
                        "unverified places must not invent a distance");
            }
        }
    }

    @Test
    @DisplayName("Category filter narrows the directory to that category only")
    void categoryFilter() {
        JsonNode food = json(client().get().uri("/api/assistant/places?category=food")
                .retrieve().body(String.class));

        assertTrue(food.size() >= 5, "the food category is the largest in the curated set");
        food.forEach(place -> assertEquals("food", place.get("category").asText()));
    }

    /* -------------------------------------------------------------- chat */

    @Test
    @DisplayName("Chat answers a food question with nearby places and a friendly reply")
    void chatAnswersFood() {
        String body = client().post().uri("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("messages", List.of(Map.of("role", "user", "content", "I'm hungry, where can I eat?"))))
                .retrieve().body(String.class);

        JsonNode reply = json(body);
        assertFalse(reply.get("reply").asText().isBlank());
        assertEquals("local", reply.get("mode").asText());
        assertTrue(reply.get("places").isArray());
        assertTrue(reply.get("places").size() > 0, "the guide offers place cards to tap");
        assertTrue(reply.get("quickReplies").size() >= 3);
    }

    @Test
    @DisplayName("A named place is recognised even with a typo, and answers include its address")
    void chatMatchesPlaceNames() {
        String body = client().post().uri("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("messages", List.of(Map.of("role", "user", "content", "asain delight"))))
                .retrieve().body(String.class);

        JsonNode reply = json(body);
        assertTrue(reply.get("reply").asText().contains("Asian Delight Shawarma"),
                "typo-tolerant matching should still find Asian Delight");
        assertTrue(reply.get("places").size() > 0, "the matched place comes back as a card");
        assertEquals("student:asian-delight-shawarma", reply.get("places").get(0).get("id").asText());
    }

    @Test
    @DisplayName("A budget question gets an honest, illustrative answer — never invented prices")
    void chatHandlesBudgets() {
        String body = client().post().uri("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("messages", List.of(Map.of("role", "user", "content", "what can I buy with R50?"))))
                .retrieve().body(String.class);

        String reply = json(body).get("reply").asText();
        assertTrue(reply.contains("R50"), "the answer echoes the budget");
        assertTrue(reply.toLowerCase().contains("not confirmed prices")
                        || reply.toLowerCase().contains("example budgets"),
                "the guide must flag that prices are illustrative");
    }

    @Test
    @DisplayName("Off-topic questions are declined instead of answered with a made-up place")
    void chatDeclinesOutOfArea() {
        String body = client().post().uri("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("messages", List.of(Map.of("role", "user", "content", "where can I eat on Mars?"))))
                .retrieve().body(String.class);

        JsonNode reply = json(body);
        assertTrue(reply.get("reply").asText().contains("Mars"));
        assertEquals(0, reply.get("places").size());
    }

    @Test
    @DisplayName("An empty message is rejected with 400 rather than crashing the assistant")
    void chatRejectsEmptyMessage() {
        var response = client().post().uri("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("messages", List.of(Map.of("role", "user", "content", "  "))))
                .exchange((request, result) -> result.getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, response);
    }

    @Test
    @DisplayName("An unknown assistant place returns 404")
    void unknownPlaceIs404() {
        var status = client().get().uri("/api/assistant/places/does-not-exist")
                .exchange((request, result) -> result.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, status);
    }
}
