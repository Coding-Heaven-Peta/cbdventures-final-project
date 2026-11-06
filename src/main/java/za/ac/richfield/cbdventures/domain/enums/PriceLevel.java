package za.ac.richfield.cbdventures.domain.enums;

/**
 * Relative cost indicator used for filtering (Phase 3 domain integrity table):
 * restricted to the defined set R, RR, RRR. No negative prices are permitted.
 */
public enum PriceLevel {
    R(1, "Budget friendly (under R80)"),
    RR(2, "Mid range (R80 - R180)"),
    RRR(3, "Premium (R180+)");

    private final int weight;
    private final String label;

    PriceLevel(int weight, String label) {
        this.weight = weight;
        this.label = label;
    }

    public int weight() {
        return weight;
    }

    public String label() {
        return label;
    }
}
