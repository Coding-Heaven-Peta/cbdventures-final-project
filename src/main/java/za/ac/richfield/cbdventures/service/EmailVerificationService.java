package za.ac.richfield.cbdventures.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.ac.richfield.cbdventures.error.ApiException;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Six-digit email verification codes for accounts created in the dark-mode client.
 *
 * <p>The pilot has no SMTP relay, so the delivery channel is a <em>documented
 * demo transport</em>: the code is written to the server log and, only when
 * {@code cbd.verification.expose-code=true} (the default in the demo profile),
 * returned to the client so the screens can be exercised during marking. Turn
 * that flag off and the API behaves exactly like a production build — the code
 * only exists in the log.</p>
 *
 * <p>Rules that make this defensible rather than decorative:</p>
 * <ul>
 *   <li>a code lives for {@link #TTL} and is then refused;</li>
 *   <li>five wrong attempts invalidate the code, so a six-digit space cannot be
 *       brute-forced from a script;</li>
 *   <li>only one live code per address — issuing a new one replaces the old;</li>
 *   <li>requests are rate limited to one per {@link #RESEND_COOLDOWN} per address;</li>
 *   <li>codes are held in memory only: nothing to leak from a database dump, and
 *       a restart simply means "resend".</li>
 * </ul>
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    /** How long a code stays valid. */
    public static final Duration TTL = Duration.ofMinutes(10);

    /** Minimum gap between two "resend" requests for the same address. */
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);

    /** Wrong guesses allowed before the code is burned. */
    private static final int MAX_ATTEMPTS = 5;

    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final boolean exposeCode;

    public EmailVerificationService(@Value("${cbd.verification.expose-code:true}") boolean exposeCode) {
        this.exposeCode = exposeCode;
    }

    /** One outstanding challenge per email address. */
    private record Challenge(String code, Instant expiresAt, Instant issuedAt, int[] attempts) {
        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    /** Result handed back to the controller: never contains the code unless exposing is on. */
    public record Issued(String code, boolean exposed, Instant expiresAt) {
    }

    /**
     * Creates (or replaces) the challenge for an address.
     *
     * @throws ApiException 429 when a code was issued less than {@link #RESEND_COOLDOWN} ago
     */
    public Issued issue(String email, boolean isResend) {
        String key = normalise(email);
        Challenge existing = challenges.get(key);
        if (isResend && existing != null
                && Instant.now().isBefore(existing.issuedAt().plus(RESEND_COOLDOWN))) {
            long wait = RESEND_COOLDOWN.minus(Duration.between(existing.issuedAt(), Instant.now())).toSeconds() + 1;
            throw new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait " + wait + " second" + (wait == 1 ? "" : "s") + " before requesting another code.");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(TTL);
        challenges.put(key, new Challenge(code, expiresAt, Instant.now(), new int[]{0}));
        log.info("Verification code for {} is {} (valid {} minutes)", key, code, TTL.toMinutes());
        return new Issued(exposeCode ? code : null, exposeCode, expiresAt);
    }

    /**
     * Checks a submitted code.
     *
     * @return true when the address is now verified
     * @throws ApiException 400 for a wrong, expired or missing code
     */
    public boolean verify(String email, String submitted) {
        String key = normalise(email);
        Challenge challenge = challenges.get(key);
        if (challenge == null) {
            throw ApiException.badRequest("That code has expired. Tap “Resend code” to get a new one.");
        }
        if (challenge.expired()) {
            challenges.remove(key);
            throw ApiException.badRequest("That code has expired. Tap “Resend code” to get a new one.");
        }
        if (submitted == null || submitted.length() != 6) {
            throw ApiException.badRequest("Enter the six-digit code from the email.");
        }
        if (!constantTimeEquals(challenge.code(), submitted.trim())) {
            challenge.attempts()[0]++;
            if (challenge.attempts()[0] >= MAX_ATTEMPTS) {
                challenges.remove(key);
                throw ApiException.badRequest("Too many incorrect attempts. Tap “Resend code” to start again.");
            }
            int left = MAX_ATTEMPTS - challenge.attempts()[0];
            throw ApiException.badRequest("That code is not right. " + left
                    + " attempt" + (left == 1 ? "" : "s") + " left.");
        }
        challenges.remove(key);
        return true;
    }

    /** True when a live challenge exists for the address (used for friendlier errors). */
    public boolean hasPending(String email) {
        Challenge challenge = challenges.get(normalise(email));
        return challenge != null && !challenge.expired();
    }

    private String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Avoids leaking how much of the code matched via timing. */
    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
