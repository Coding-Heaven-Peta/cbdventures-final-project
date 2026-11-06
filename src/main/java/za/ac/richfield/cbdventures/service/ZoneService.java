package za.ac.richfield.cbdventures.service;

import org.springframework.stereotype.Service;
import za.ac.richfield.cbdventures.config.ZoneProperties;
import za.ac.richfield.cbdventures.domain.enums.ZoneRadius;
import za.ac.richfield.cbdventures.error.ApiException;
import za.ac.richfield.cbdventures.util.GeoUtils;
import za.ac.richfield.cbdventures.web.dto.ZoneView;

/**
 * Owns the geographic boundary rule: every listing must fall inside the pilot
 * zone anchored on the Richfield campus. The 1 km primary zone is the MVP; the
 * 5 km extended zone can be switched on with configuration only.
 */
@Service
public class ZoneService {

    private final ZoneProperties props;

    public ZoneService(ZoneProperties props) {
        this.props = props;
    }

    public ZoneView view() {
        return new ZoneView(props.anchorName(), props.anchorAddress(),
                props.anchorLatitude(), props.anchorLongitude(),
                props.primaryRadiusKm(), props.extendedRadiusKm(), props.extendedEnabled());
    }

    public double anchorLatitude() {
        return props.anchorLatitude();
    }

    public double anchorLongitude() {
        return props.anchorLongitude();
    }

    public double primaryRadiusKm() {
        return props.primaryRadiusKm();
    }

    public boolean extendedEnabled() {
        return props.extendedEnabled();
    }

    /** Distance in km from the Richfield anchor point. */
    public double distanceFromAnchor(double latitude, double longitude) {
        return GeoUtils.distanceKm(props.anchorLatitude(), props.anchorLongitude(), latitude, longitude);
    }

    /**
     * Classifies a coordinate into a zone, rejecting anything outside the widest
     * allowed boundary (MVP + extended).
     *
     * @throws ApiException when the coordinate falls outside the 5 km boundary
     */
    public ZoneRadius classify(double latitude, double longitude) {
        double km = distanceFromAnchor(latitude, longitude);
        if (km > props.extendedRadiusKm()) {
            throw ApiException.badRequest(String.format(
                    "This address is %.1f km from Richfield, which is outside the %.0f km CBD boundary. "
                            + "Please use an address inside the pilot zone.",
                    km, props.extendedRadiusKm()));
        }
        return km <= props.primaryRadiusKm() ? ZoneRadius.PRIMARY_1KM : ZoneRadius.EXTENDED_5KM;
    }

    /**
     * A listing is publicly discoverable when it is inside the currently enabled
     * boundary. While the extended zone is switched off, only 1 km listings appear.
     */
    public boolean isDiscoverable(ZoneRadius radius) {
        return radius == ZoneRadius.PRIMARY_1KM || props.extendedEnabled();
    }

    /** The radius used for public browsing right now. */
    public double activeRadiusKm() {
        return props.extendedEnabled() ? props.extendedRadiusKm() : props.primaryRadiusKm();
    }
}
