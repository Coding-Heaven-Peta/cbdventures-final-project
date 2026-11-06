package za.ac.richfield.cbdventures.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4 — unit tests for the recommendation scoring rules (FR-04).
 */
class ScoringTest {

    @Test
    @DisplayName("A well reviewed, close, complete listing beats a sparse far one")
    void strongerListingScoresHigher() {
        double strong = Scoring.baseScore(5.0, 12, 0.2, true, true, 4);
        double weak = Scoring.baseScore(2.0, 0, 1.0, false, false, 0);
        assertTrue(strong > weak);
        assertTrue(strong <= 100.0, "score must stay within 100 points");
    }

    @Test
    @DisplayName("Closer listings score higher when everything else is equal")
    void proximityMatters() {
        double near = Scoring.baseScore(4.0, 5, 0.1, true, true, 2);
        double far = Scoring.baseScore(4.0, 5, 1.0, true, true, 2);
        assertTrue(near > far);
    }

    @Test
    @DisplayName("Popularity is capped so one viral listing cannot dominate")
    void popularityIsCapped() {
        double twenty = Scoring.baseScore(4.0, 20, 0.5, true, true, 2);
        double thousand = Scoring.baseScore(4.0, 1000, 0.5, true, true, 2);
        assertEquals(twenty, thousand, 1e-9);
    }

    @Test
    @DisplayName("A null distance falls back to a neutral proximity score")
    void nullDistanceIsNeutral() {
        double score = Scoring.baseScore(4.0, 5, null, true, true, 2);
        assertTrue(score > 0);
    }

    @Test
    @DisplayName("Personal affinity adds at most 30 points")
    void affinityBonusIsBounded() {
        double base = 50.0;
        assertEquals(base, Scoring.personalised(base, 0), 1e-9);
        assertEquals(base + 30, Scoring.personalised(base, 1), 1e-9);
        assertEquals(base + 30, Scoring.personalised(base, 5), 1e-9, "affinity is clamped to 1");
    }

    @Test
    @DisplayName("Affinity is normalised against the strongest category weight")
    void affinityNormalisation() {
        Map<Long, Double> weights = Map.of(1L, 1.0, 2L, 4.0);
        assertEquals(1.0, Scoring.affinityFor(weights, 2L), 1e-9);
        assertEquals(0.25, Scoring.affinityFor(weights, 1L), 1e-9);
        assertEquals(0.0, Scoring.affinityFor(weights, 99L), 1e-9);
        assertEquals(0.0, Scoring.affinityFor(Map.of(), 1L), 1e-9);
    }
}
