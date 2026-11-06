package za.ac.richfield.cbdventures.assistant;

import java.util.List;

/**
 * One entry in the student assistant directory.
 *
 * <p>Ported from the CBDVentures Assistant project (Node) so the final build runs
 * from a single jar with no second process. A place is only "nearby" when its
 * stored coordinates put it inside the 1 km campus radius; anything else is
 * surfaced as student-submitted and awaiting verification.</p>
 */
public record AssistantPlace(
        String id,
        String name,
        List<String> aliases,
        String category,
        String address,
        String description,
        List<String> highlights,
        String image,
        String source,
        String verificationNote,
        String submittedAddress,
        Double latitude,
        Double longitude,
        Integer distanceMeters,
        boolean withinRadius,
        String directionsUrl,
        String mapSearchUrl) {

    /** Straight-line distance as the guide phrases it. */
    public String distanceText() {
        if (!withinRadius || distanceMeters == null) return null;
        return "about " + distanceMeters + " m from campus";
    }
}
