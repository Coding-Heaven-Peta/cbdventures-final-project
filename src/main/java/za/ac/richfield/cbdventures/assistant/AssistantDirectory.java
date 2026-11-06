package za.ac.richfield.cbdventures.assistant;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Loads the curated student directory and applies the same eligibility rule the
 * assistant project used: a place counts as "nearby" only when its coordinates
 * fall inside the 1 km campus radius.
 */
@Component
public class AssistantDirectory {

    private static final Logger LOG = LoggerFactory.getLogger(AssistantDirectory.class);

    /** Richfield Pretoria Campus reference pin (matches the assistant project). */
    public static final double CAMPUS_LAT = -25.746467;
    public static final double CAMPUS_LNG = 28.19252;
    public static final String CAMPUS_ADDRESS = "291 Helen Joseph Street, Pretoria, 0001";
    public static final int RADIUS_METRES = 1000;
    private static final double EARTH_RADIUS_METRES = 6_371_008.8;
    private static final String DIRECTIONS_ORIGIN = "291 Helen Joseph Street, Pretoria Central, Pretoria, South Africa";

    /** The nine categories the guide understands, in display order. */
    public static final List<Category> CATEGORIES = List.of(
            new Category("food", "Food & coffee", "food"),
            new Category("groceries", "Groceries", "basket"),
            new Category("health", "Health & pharmacy", "heart"),
            new Category("printing", "Printing & stationery", "printer"),
            new Category("study", "Libraries & study", "study"),
            new Category("banking", "Banks & ATMs", "wallet"),
            new Category("grooming", "Barbers & salons", "sparkle"),
            new Category("transport", "Transport", "bus"),
            new Category("fun", "Entertainment", "ticket"));

    public record Category(String id, String label, String iconKey) { }

    private final List<AssistantPlace> all;
    private final Set<String> categoryIds;

    public AssistantDirectory(ObjectMapper mapper) {
        Set<String> ids = new LinkedHashSet<>();
        CATEGORIES.forEach(category -> ids.add(category.id()));
        this.categoryIds = Set.copyOf(ids);
        this.all = load(mapper);
    }

    /** Places inside the radius, nearest first. */
    public List<AssistantPlace> nearby() {
        return all.stream()
                .filter(AssistantPlace::withinRadius)
                .sorted(Comparator.comparingInt(AssistantPlace::distanceMeters))
                .toList();
    }

    /** Student-submitted entries whose coordinates are missing or outside the radius. */
    public List<AssistantPlace> pending() {
        return all.stream()
                .filter(place -> !place.withinRadius())
                .sorted(Comparator.comparing(AssistantPlace::name))
                .toList();
    }

    /** Everything, for name matching. */
    public List<AssistantPlace> everything() {
        return all;
    }

    public List<AssistantPlace> byCategory(String categoryId) {
        if (categoryId == null || !categoryIds.contains(categoryId)) return List.of();
        return all.stream().filter(place -> place.category().equals(categoryId)).toList();
    }

    public boolean isCategory(String value) {
        return value != null && categoryIds.contains(value);
    }

    public static String labelFor(String categoryId) {
        return CATEGORIES.stream()
                .filter(category -> category.id().equals(categoryId))
                .map(Category::label)
                .findFirst()
                .orElse("places");
    }

    /* ------------------------------------------------------------- loading */

    private List<AssistantPlace> load(ObjectMapper mapper) {
        try (InputStream in = new ClassPathResource("assistant/places.json").getInputStream()) {
            JsonNode root = mapper.readTree(in);
            List<AssistantPlace> places = new ArrayList<>();
            for (JsonNode node : root) {
                AssistantPlace place = normalise(node);
                if (place != null) places.add(place);
            }
            return List.copyOf(places);
        } catch (IOException | RuntimeException failure) {
            LOG.warn("Assistant directory could not be loaded: {}", failure.getMessage());
            // The assistant degrades to "no directory" rather than breaking the app.
            return List.of();
        }
    }

    private AssistantPlace normalise(JsonNode raw) {
        String name = text(raw, "name");
        String category = text(raw, "category");
        if (name == null || name.isBlank() || !categoryIds.contains(category)) return null;

        Double lat = number(raw, "lat");
        Double lng = number(raw, "lng");
        boolean mapped = lat != null && lng != null
                && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
        double distance = mapped ? distanceMetres(CAMPUS_LAT, CAMPUS_LNG, lat, lng) : Double.POSITIVE_INFINITY;
        boolean within = mapped && distance <= RADIUS_METRES + 1e-7;

        String address = orDefault(text(raw, "address"), "Precise address not supplied");
        String image = text(raw, "image");
        // The handover stored images at /images/*.jpg; the app serves them from /images/places/.
        String imagePath = image != null && image.startsWith("/images/") && !image.startsWith("/images/places/")
                ? image.replace("/images/", "/images/places/")
                : image;

        return new AssistantPlace(
                orDefault(text(raw, "id"), "curated:" + name),
                name,
                strings(raw, "aliases"),
                category,
                address,
                orDefault(text(raw, "description"), ""),
                strings(raw, "highlights"),
                imagePath,
                orDefault(text(raw, "sourceLabel"), "Community submitted"),
                orDefault(text(raw, "verificationNote"), ""),
                orDefault(text(raw, "submittedAddress"), ""),
                mapped ? lat : null,
                mapped ? lng : null,
                mapped ? (int) Math.round(distance) : null,
                within,
                directionsUrl(within ? lat + "," + lng : address + ", South Africa"),
                "https://www.google.com/maps/search/?api=1&query="
                        + encode(name + " " + address));
    }

    /** Walking directions that always start at the campus gate. */
    public static String directionsUrl(String destination) {
        return "https://www.google.com/maps/dir/?api=1&origin=" + encode(DIRECTIONS_ORIGIN)
                + "&destination=" + encode(destination) + "&travelmode=walking";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** Haversine distance in metres. */
    public static double distanceMetres(double fromLat, double fromLng, double toLat, double toLng) {
        double toRad = Math.PI / 180;
        double dLat = (toLat - fromLat) * toRad;
        double dLng = (toLng - fromLng) * toRad;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(fromLat * toRad) * Math.cos(toLat * toRad)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_METRES * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Double number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        if (value.isNumber()) return value.asDouble();
        try {
            String raw = value.asText();
            return raw.isBlank() ? null : Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static List<String> strings(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) return List.of();
        List<String> out = new ArrayList<>();
        value.forEach(item -> {
            if (item.isTextual() && !item.asText().isBlank()) out.add(item.asText().trim());
        });
        return List.copyOf(out);
    }

    private static String orDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }

    /** Lowercase, accent-free, punctuation-free form used for name matching. */
    public static String normaliseWords(String value) {
        if (value == null) return "";
        String lower = value.toLowerCase(Locale.ROOT)
                .replace('’', ' ').replace('‘', ' ').replace('`', ' ').replace('\'', ' ');
        StringBuilder out = new StringBuilder(lower.length());
        for (char c : lower.toCharArray()) {
            out.append(Character.isLetterOrDigit(c) ? c : ' ');
        }
        return out.toString().replaceAll("\\s+", " ").trim();
    }
}
