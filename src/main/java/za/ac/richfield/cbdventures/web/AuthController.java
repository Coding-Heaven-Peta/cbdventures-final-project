package za.ac.richfield.cbdventures.web;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.richfield.cbdventures.config.OAuthProviderSettings;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.security.CurrentUser;
import za.ac.richfield.cbdventures.service.AuthService;
import za.ac.richfield.cbdventures.web.dto.AuthDtos;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuthProviderSettings oauthProviderSettings;

    public AuthController(AuthService authService, OAuthProviderSettings oauthProviderSettings) {
        this.authService = authService;
        this.oauthProviderSettings = oauthProviderSettings;
    }

    /** Creates the account and sends a six-digit code; the client then shows the verify screen. */
    @PostMapping("/register")
    public ResponseEntity<AuthDtos.RegisterResult> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /** Exchanges the six-digit code for a session token. */
    @PostMapping("/verify")
    public AuthDtos.AuthResponse verify(@Valid @RequestBody AuthDtos.VerifyRequest request) {
        return authService.verify(request);
    }

    /** Re-sends a code (rate limited to one every 30 seconds per address). */
    @PostMapping("/resend")
    public AuthDtos.ResendResult resend(@Valid @RequestBody AuthDtos.ResendRequest request) {
        return authService.resend(request);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/providers")
    public Map<String, Boolean> providers() {
        return Map.of("google", oauthProviderSettings.googleEnabled(),
                "apple", oauthProviderSettings.appleEnabled());
    }

    @PostMapping("/oauth/exchange")
    public ResponseEntity<AuthDtos.AuthResponse> exchangeOAuth(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object stored = session == null ? null : session.getAttribute("cbd.oauth.auth-response");
        if (!(stored instanceof AuthDtos.AuthResponse authResponse)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        session.invalidate();
        return ResponseEntity.ok(authResponse);
    }

    /** Returns the signed-in user, or 204 when the visitor is browsing as a guest. */
    @GetMapping("/me")
    public ResponseEntity<AuthDtos.UserView> me() {
        return CurrentUser.optional()
                .map(user -> ResponseEntity.ok(authService.toView(user)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        User user = CurrentUser.require();
        authService.changePassword(user, request);
        return ResponseEntity.noContent().build();
    }
}
