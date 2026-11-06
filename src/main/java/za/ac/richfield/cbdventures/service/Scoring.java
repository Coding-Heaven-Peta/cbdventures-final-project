package za.ac.richfield.cbdventures.service;

import java.util.Map;

/**
 * Transparent, explainable ranking used for "recommended" sorting and for the
 * personalised suggestion strip on the home screen (FR-04).
 *
 * <p>Each listing scores out of 100:</p>
 * <ul>
 *   <li>Quality  - up to 40 points from the average star rating</li>
 *   <li>Popularity - up to 15 points from the number of reviews</li>
 *   <li>Proximity - up to 25 points, decaying with distance from the reference point</li>
 *   <li>Completeness - up to 20 points for photos, contact details and amenities</li>
 * </ul>
 * A personalised affinity bonus (up to 30) is added when the visitor has saved
 * preferences or has reviewed businesses in that category.
 */
public final class Scoring {

    private Scoring() {
    }

    public static double baseScore(double rating, long reviewCount, Double distanceKm,
                                   boolean hasPhoto, boolean hasContact, int amenityCount) {
        double quality = (rating / 5.0) * 40.0;
        double popularity = Math.min(reviewCount, 20) / 20.0 * 15.0;
        double proximity = distanceKm == null ? 10.0 : Math.exp(-Math.max(distanceKm, 0) / 0.8) * 25.0;
        double completeness = (hasPhoto ? 8.0 : 0.0) + (hasContact ? 5.0 : 0.0)
                + Math.min(amenityCount, 4) / 4.0 * 7.0;
        return quality + popularity + proximity + completeness;
    }

    /**
     * @param categoryAffinity how strongly this visitor leans towards the category (0..1)
     */
    public static double personalised(double baseScore, double categoryAffinity) {
        return baseScore + Math.min(Math.max(categoryAffinity, 0), 1) * 30.0;
    }

    /** Normalises raw category weights into 0..1 so the bonus stays bounded. */
    public static double affinityFor(Map<Long, Double> weights, Long categoryId) {
        if (weights == null || weights.isEmpty() || categoryId == null) {
            return 0.0;
        }
        double max = weights.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double value = weights.getOrDefault(categoryId, 0.0);
        return max <= 0 ? 0.0 : value / max;
    }
}
