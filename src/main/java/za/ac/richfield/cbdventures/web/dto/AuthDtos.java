package za.ac.richfield.cbdventures.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import za.ac.richfield.cbdventures.domain.enums.Role;

/**
 * Request/response payloads for authentication.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "is required")
            @Size(max = 100, message = "must be 100 characters or fewer")
            String fullName,

            @NotBlank(message = "is required")
            @Email(message = "must be a valid email address")
            String email,

            @NotBlank(message = "is required")
            @Size(min = 8, max = 100, message = "must be at least 8 characters")
            String password,

            @NotNull(message = "is required")
            Role role,

            /** Optional business name captured when an owner registers. */
            String businessName
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "is required") @Email(message = "must be a valid email address") String email,
            @NotBlank(message = "is required") String password
    ) {
    }

    public record AuthResponse(String token, UserView user) {
    }

    /**
     * What registration returns now: no token until the address is confirmed.
     *
     * @param demoCode the six-digit code, present only while the demo transport
     *                 is enabled ({@code cbd.verification.expose-code=true})
     */
    public record RegisterResult(String email, String fullName, boolean verificationRequired,
                                 String demoCode, int expiresInMinutes) {
    }

    public record VerifyRequest(
            @NotBlank(message = "is required") @Email(message = "must be a valid email address") String email,
            @NotBlank(message = "is required")
            @Pattern(regexp = "\\d{6}", message = "must be the six-digit code") String code
    ) {
    }

    public record ResendRequest(
            @NotBlank(message = "is required") @Email(message = "must be a valid email address") String email
    ) {
    }

    public record ResendResult(boolean sent, String demoCode, int expiresInMinutes) {
    }

    public record UserView(
            Long id,
            String fullName,
            String email,
            Role role,
            String initials,
            int businessCount,
            boolean emailVerified
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "is required") String currentPassword,
            @NotBlank(message = "is required")
            @Size(min = 8, message = "must be at least 8 characters")
            @Pattern(regexp = ".*[0-9].*", message = "must contain at least one number")
            String newPassword
    ) {
    }
}
