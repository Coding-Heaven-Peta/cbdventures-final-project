package za.ac.richfield.cbdventures.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.domain.enums.Role;
import za.ac.richfield.cbdventures.error.ApiException;
import za.ac.richfield.cbdventures.repo.BusinessRepository;
import za.ac.richfield.cbdventures.repo.UserRepository;
import za.ac.richfield.cbdventures.security.JwtService;
import za.ac.richfield.cbdventures.web.dto.AuthDtos;

import java.util.UUID;
import java.util.Locale;

/**
 * Registration, login and profile. Passwords are BCrypt hashed and never
 * returned by any endpoint (NFR: Security / POPIA data minimisation).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService verificationService;

    public AuthService(UserRepository userRepository,
                       BusinessRepository businessRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EmailVerificationService verificationService) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.verificationService = verificationService;
    }

    /**
     * Creates the account unverified and sends a six-digit code. No token is
     * issued yet: the visitor must confirm the address first, which is what the
     * "Verify your email" screen does.
     */
    @Transactional
    public AuthDtos.RegisterResult register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email address already exists. Try signing in instead.");
        }
        if (request.role() == Role.ADMIN) {
            throw ApiException.forbidden("Administrator accounts are created by the platform team.");
        }
        User user = new User(request.fullName().trim(), email,
                passwordEncoder.encode(request.password()), request.role());
        user.setEmailVerified(false);
        userRepository.save(user);

        EmailVerificationService.Issued issued = verificationService.issue(email, false);
        return new AuthDtos.RegisterResult(email, user.getFullName(), true, issued.code(),
                (int) EmailVerificationService.TTL.toMinutes());
    }

    /** Confirms the code and, on success, returns the session token. */
    @Transactional
    public AuthDtos.AuthResponse verify(AuthDtos.VerifyRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.badRequest("That email address is not registered."));
        if (user.isEmailVerified()) {
            return new AuthDtos.AuthResponse(jwtService.issueToken(user), toView(user));
        }
        verificationService.verify(email, request.code());
        user.setEmailVerified(true);
        userRepository.save(user);
        return new AuthDtos.AuthResponse(jwtService.issueToken(user), toView(user));
    }

    /** Re-issues a code, rate limited per address. */
    @Transactional(readOnly = true)
    public AuthDtos.ResendResult resend(AuthDtos.ResendRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.badRequest("That email address is not registered."));
        if (user.isEmailVerified()) {
            throw ApiException.badRequest("This address is already verified. You can sign in.");
        }
        EmailVerificationService.Issued issued = verificationService.issue(email, true);
        return new AuthDtos.ResendResult(true, issued.code(), (int) EmailVerificationService.TTL.toMinutes());
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.badRequest("That email address and password do not match."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.badRequest("That email address and password do not match.");
        }
        if (!user.isEmailVerified()) {
            // Tells the client to show the code screen, and re-sends if the old code lapsed.
            if (!verificationService.hasPending(email)) {
                verificationService.issue(email, false);
            }
            throw ApiException.forbidden("Confirm your email address to finish setting up your account.");
        }
        return new AuthDtos.AuthResponse(jwtService.issueToken(user), toView(user));
    }

    /** Signs in a provider-verified email or creates its first visitor account. */
    @Transactional
    public AuthDtos.AuthResponse loginWithProvider(String email, String fullName) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElseGet(() -> {
            String safeName = fullName == null || fullName.isBlank()
                    ? normalizedEmail.substring(0, normalizedEmail.indexOf('@')) : fullName.trim();
            User created = new User(safeName, normalizedEmail,
                    passwordEncoder.encode(UUID.randomUUID().toString()), Role.VISITOR);
            created.setEmailVerified(true);
            return userRepository.save(created);
        });
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }
        return new AuthDtos.AuthResponse(jwtService.issueToken(user), toView(user));
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserView toView(User user) {
        long businesses = user.getRole() == Role.VISITOR
                ? 0
                : businessRepository.findByOwnerIdOrderByNameAsc(user.getId()).size();
        return new AuthDtos.UserView(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                BusinessMapper.initials(user.getFullName()), (int) businesses, user.isEmailVerified());
    }

    @Transactional
    public void changePassword(User user, AuthDtos.ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Your current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
