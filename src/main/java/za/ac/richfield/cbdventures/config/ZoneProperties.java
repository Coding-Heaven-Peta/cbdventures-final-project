package za.ac.richfield.cbdventures.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Geographic boundary configuration. The anchor point is the Richfield Graduate
 * Institute of Technology Pretoria campus (291 Helen Joseph Street, opposite
 * Sammy Marks Square). Everything is externalised so the pilot zone can be
 * widened to 5 km without touching code (Phase 5: "expand to the extended zone").
 */
@ConfigurationProperties(prefix = "cbd.zone")
public record ZoneProperties(
        String anchorName,
        String anchorAddress,
        double anchorLatitude,
        double anchorLongitude,
        double primaryRadiusKm,
        double extendedRadiusKm,
        boolean extendedEnabled
) {
}
