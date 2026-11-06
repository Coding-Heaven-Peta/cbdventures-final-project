package za.ac.richfield.cbdventures.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.error.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Optional;

/**
 * Convenience access to the currently authenticated user.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<User> optional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static User require() {
        return optional().orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in to continue."));
    }
}
