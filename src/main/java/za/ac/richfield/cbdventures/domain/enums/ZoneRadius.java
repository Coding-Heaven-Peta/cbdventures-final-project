package za.ac.richfield.cbdventures.domain.enums;

/**
 * Spatial boundaries (Phase 3, Step 1): 1KM_PRIMARY is the MVP zone anchored on
 * Richfield Graduate Institute of Technology. 5KM_EXTENDED is reserved for the
 * post-MVP release and is hidden from public discovery while
 * {@code cbd.zone.extended-enabled} is false.
 */
public enum ZoneRadius {
    PRIMARY_1KM(1.0),
    EXTENDED_5KM(5.0);

    private final double km;

    ZoneRadius(double km) {
        this.km = km;
    }

    public double km() {
        return km;
    }
}
