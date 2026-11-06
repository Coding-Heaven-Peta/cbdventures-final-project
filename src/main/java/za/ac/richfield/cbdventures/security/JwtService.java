package za.ac.richfield.cbdventures.security;

import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import za.ac.richfield.cbdventures.config.JwtProperties;
import za.ac.richfield.cbdventures.domain.User;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal, dependency-free HS256 JSON Web Token implementation.
 *
 * <p>A JWT is three Base64Url segments: header.payload.signature, where the
 * signature is an HMAC-SHA256 over "header.payload" using the shared secret.
 * Only the JDK crypto API is used, which keeps the dependency surface small and
 * makes the token format easy to demonstrate in the project report.</p>
 */
@Service
public class JwtService {

    private static final Base64.Encoder B64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_URL_DECODER = Base64.getUrlDecoder();
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final byte[] secret;
    private final long ttlSeconds;
    private final ObjectMapper mapper = new ObjectMapper();

    public JwtService(JwtProperties properties) {
        this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = properties.ttlMinutes() * 60;
    }

    public String issueToken(User user) {
        Instant now = Instant.now();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", String.valueOf(user.getId()));
        claims.put("email", user.getEmail());
        claims.put("name", user.getFullName());
        claims.put("role", user.getRole().name());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plusSeconds(ttlSeconds).getEpochSecond());

        try {
            String header = B64_URL_ENCODER.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
            String payload = B64_URL_ENCODER.encodeToString(
                    mapper.writeValueAsBytes(claims));
            String signingInput = header + "." + payload;
            String signature = B64_URL_ENCODER.encodeToString(hmac(signingInput));
            return signingInput + "." + signature;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to issue authentication token", ex);
        }
    }

    /**
     * @return the decoded claims when the signature and expiry are valid,
     * otherwise {@code null}.
     */
    public Map<String, Object> parseToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        String signingInput = parts[0] + "." + parts[1];
        byte[] expected = hmac(signingInput);
        byte[] actual;
        try {
            actual = B64_URL_DECODER.decode(parts[2]);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        if (!MessageDigest.isEqual(expected, actual)) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = mapper.readValue(B64_URL_DECODER.decode(parts[1]), Map.class);
            Object exp = claims.get("exp");
            if (exp instanceof Number number && number.longValue() < Instant.now().getEpochSecond()) {
                return null;
            }
            return claims;
        } catch (Exception ex) {
            return null;
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign authentication token", ex);
        }
    }
}
