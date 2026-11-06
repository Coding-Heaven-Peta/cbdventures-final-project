package za.ac.richfield.cbdventures.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import za.ac.richfield.cbdventures.domain.*;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.domain.enums.PriceLevel;
import za.ac.richfield.cbdventures.domain.enums.Role;
import za.ac.richfield.cbdventures.repo.*;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4 — end-to-end HTTP tests for the REST contract, JWT authentication and
 * the security rules (guests may browse, only signed-in users may review, only
 * owners may manage listings).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiHttpTest {

    @LocalServerPort
    int port;

    @Autowired CategoryRepository categoryRepository;
    @Autowired BusinessRepository businessRepository;
    @Autowired UserRepository userRepository;
    @Autowired ReviewRepository reviewRepository;

    private final ObjectMapper mapper = new ObjectMapper();
    private RestClient client;
    private Long businessId;

    @BeforeEach
    void setUp() {
        client = RestClient.builder().baseUrl("http://localhost:" + port).build();

        if (categoryRepository.count() == 0) {
            Category food = categoryRepository.save(new Category("Food & Drinks", "food-drinks", "Quick eats", "food", 1));
            User owner = userRepository.save(new User("HTTP Owner", "http-owner@test.co.za", "{bcrypt}", Role.OWNER));
            Business business = new Business();
            business.setName("HTTP Kota");
            business.setOwner(owner);
            business.setCategory(food);
            business.setAddress("1 Test Street, Pretoria Central");
            business.setDescription("Seeded by ApiHttpTest");
            business.setLatitude(BigDecimal.valueOf(-25.7465));
            business.setLongitude(BigDecimal.valueOf(28.1927));
            business.setPriceLevel(PriceLevel.R);
            business.setOperatingHours("Mon-Fri 08:00 - 17:00");
            business.setStatus(BusinessStatus.ACTIVE);
            businessId = businessRepository.save(business).getId();
        } else {
            businessId = businessRepository.findAll().getFirst().getId();
        }
    }

    private JsonNode get(String path) {
        String body = client.get().uri(path).retrieve().body(String.class);
        return mapper.readTree(body);
    }

    @Test
    @DisplayName("Health endpoint reports UP")
    void health() {
        assertEquals("UP", get("/actuator/health").path("status").asString());
    }

    @Test
    @DisplayName("The single page app shell is served at the site root")
    void indexIsServed() {
        String html = client.get().uri("/").retrieve().body(String.class);
        assertTrue(html.contains("CBDVentures"));
        assertTrue(html.contains("/js/app.js"));
    }

    @Test
    @DisplayName("Guests can browse categories, listings and the home payload")
    void publicDiscovery() {
        JsonNode categories = get("/api/categories");
        assertTrue(categories.isArray());
        assertTrue(categories.size() >= 1);

        JsonNode listing = get("/api/businesses/" + businessId);
        assertEquals("HTTP Kota", listing.path("name").asString());
        assertTrue(listing.has("reviews"));
        assertTrue(listing.has("rating"));

        JsonNode home = get("/api/discover/home");
        assertTrue(home.has("categories"));
        assertTrue(home.has("stats"));
        assertTrue(home.path("zone").has("primaryRadiusKm"));
    }

    @Test
    @DisplayName("Zone map pins carry coordinates inside the browsing radius")
    void mapPins() {
        JsonNode pins = get("/api/businesses/map");
        assertTrue(pins.isArray());
        assertTrue(pins.size() >= 1, "at least one active listing should be plottable");

        JsonNode pin = pins.get(0);
        for (String field : new String[]{"id", "name", "latitude", "longitude", "distanceKm"}) {
            assertTrue(pin.has(field), "pin is missing " + field);
        }
        pins.forEach(node -> assertTrue(node.path("distanceKm").asDouble() <= 1.0,
                "pins must sit inside the 1 km pilot zone"));
    }

    @Test
    @DisplayName("A malformed listing id returns 400 instead of 500")
    void badListingIdIsClientError() {
        int status = client.get().uri("/api/businesses/not-a-number")
                .exchange((request, response) -> response.getStatusCode().value());
        assertEquals(400, status);
    }

    @Test
    @DisplayName("Filter parameters are applied and echoed back")
    void filtersAreApplied() {
        JsonNode results = get("/api/businesses?category=food-drinks&sort=distance&maxDistanceKm=1");
        assertTrue(results.has("total"));
        assertTrue(results.path("appliedFilters").has("reference"));
        assertEquals("distance", results.path("sort").asString());
    }

    @Test
    @DisplayName("Registration issues a six-digit code, verification returns the token")
    void registerThenVerify() {
        String registerBody = """
                {"fullName":"HTTP Student","email":"http-student@test.co.za",
                 "password":"Password123","role":"VISITOR"}""";

        JsonNode registered = mapper.readTree(client.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).body(registerBody)
                .retrieve().body(String.class));
        assertTrue(registered.path("verificationRequired").asBoolean(),
                "a new account must confirm its email address first");
        String code = registered.path("demoCode").asString();
        assertEquals(6, code.length(), "the demo transport returns a six-digit code");
        assertFalse(registered.has("token"), "no session before the address is confirmed");

        JsonNode verified = mapper.readTree(client.post().uri("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"email\":\"http-student@test.co.za\",\"code\":\"" + code + "\"}")
                .retrieve().body(String.class));
        String token = verified.path("token").asString();
        assertFalse(token.isBlank(), "a JWT must be issued once the code is accepted");
        assertEquals(3, token.split("\\.").length, "the token must have three JWT segments");
        assertTrue(verified.path("user").path("emailVerified").asBoolean());
        assertFalse(verified.path("user").has("password"), "passwords must never be serialised");

        String loginBody = """
                {"email":"http-student@test.co.za","password":"Password123"}""";
        JsonNode loggedIn = mapper.readTree(client.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).body(loginBody)
                .retrieve().body(String.class));
        assertEquals("VISITOR", loggedIn.path("user").path("role").asString());
    }

    @Test
    @DisplayName("An unverified account cannot sign in and a wrong code is refused")
    void verificationGuardsSignIn() {
        client.post().uri("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"fullName":"Unverified","email":"unverified@test.co.za",
                         "password":"Password123","role":"VISITOR"}""")
                .retrieve().body(String.class);

        int blocked = client.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"email":"unverified@test.co.za","password":"Password123"}""")
                .exchange((request, response) -> response.getStatusCode().value());
        assertEquals(403, blocked, "sign-in waits for the address to be confirmed");

        int wrongCode = client.post().uri("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"email":"unverified@test.co.za","code":"000000"}""")
                .exchange((request, response) -> response.getStatusCode().value());
        assertEquals(400, wrongCode, "a wrong code is a validation error, not a server error");
    }

    /** Registers an account and returns the bearer token after verifying it. */
    private String registerVerified(String fullName, String email, String role) {
        String body = """
                {"fullName":"%s","email":"%s","password":"Password123","role":"%s"}"""
                .formatted(fullName, email, role);
        JsonNode registered = mapper.readTree(client.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class));
        String code = registered.path("demoCode").asString();
        JsonNode verified = mapper.readTree(client.post().uri("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}")
                .retrieve().body(String.class));
        return verified.path("token").asString();
    }

    @Test
    @DisplayName("A guest cannot post a review (401), a signed-in visitor can (201)")
    void reviewRequiresAuthentication() {
        String reviewBody = """
                {"rating":5,"comment":"Posted from the HTTP test"}""";

        int guestStatus = client.post().uri("/api/businesses/" + businessId + "/reviews")
                .contentType(MediaType.APPLICATION_JSON).body(reviewBody)
                .exchange((request, response) -> response.getStatusCode().value());
        assertEquals(401, guestStatus, "guests receive 401 so the UI can open the sign-in dialog");

        String token = registerVerified("HTTP Reviewer", "http-reviewer@test.co.za", "VISITOR");

        int created = client.post().uri("/api/businesses/" + businessId + "/reviews")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).body(reviewBody)
                .exchange((request, response) -> response.getStatusCode().value());
        assertEquals(201, created);
    }

    @Test
    @DisplayName("Validation failures return a readable message")
    void validationMessages() {
        String body = client.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"fullName":"","email":"not-an-email","password":"short","role":"VISITOR"}""")
                .exchange((request, response) -> response.bodyTo(String.class));
        JsonNode error = mapper.readTree(body);
        assertEquals(400, error.path("status").asInt());
        assertTrue(error.path("message").asString().toLowerCase().contains("email"));
    }

    @Test
    @DisplayName("Only administrators reach the verification queue")
    void adminEndpointsAreProtected() {
        int guest = client.get().uri("/api/admin/stats")
                .exchange((request, response) -> response.getStatusCode().value());
        assertEquals(401, guest);

        String token = registerVerified("Not Admin", "not-admin@test.co.za", "VISITOR");

        int forbidden = client.get().uri("/api/admin/stats")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange((request, response) -> response.getStatusCode().value());
        assertEquals(403, forbidden);
    }

    @Test
    @DisplayName("A tampered token is rejected")
    void tamperedTokenIsRejected() {
        int status = client.get().uri("/api/preferences")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.validtoken")
                .exchange((request, response) -> response.getStatusCode().value());
        assertEquals(401, status);
    }

    @Test
    @DisplayName("A malformed or unrecognised registration value answers 400, never 500")
    void invalidRegistrationBodyIsAClientError() {
        String body = "{\"fullName\":\"Bad Role\",\"email\":\"bad-role@test.co.za\","
                + "\"password\":\"Password123!\",\"role\":\"STUDENT\"}";

        var status = client.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((request, response) -> response.getStatusCode());

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, status,
                "an unknown role is the caller's mistake, so the API must not return 500");
    }

    @Test
    @DisplayName("Business creation is rejected outside the pilot zone")
    void zoneValidationOverHttp() {
        String token = registerVerified("Zone Owner", "zone-owner@test.co.za", "OWNER");

        Long categoryId = categoryRepository.findAll().getFirst().getId();
        Map<String, Object> payload = Map.of(
                "name", "Johannesburg Listing",
                "address", "1 Elsewhere Road, Johannesburg",
                "latitude", -26.2041,
                "longitude", 28.0473,
                "priceLevel", "R",
                "categoryId", categoryId);

        String errorBody = client.post().uri("/api/owner/businesses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange((request, response) -> response.bodyTo(String.class));
        JsonNode error = mapper.readTree(errorBody);

        assertEquals(400, error.path("status").asInt());
        assertTrue(error.path("message").asString().contains("outside the 5 km CBD boundary"));
    }
}
