package za.ac.richfield.cbdventures.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cbd.security.jwt")
public record JwtProperties(
        String secret,
        long ttlMinutes,
        String issuer
) {
}
