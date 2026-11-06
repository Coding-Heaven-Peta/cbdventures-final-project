package za.ac.richfield.cbdventures.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import za.ac.richfield.cbdventures.service.AuthService;
import za.ac.richfield.cbdventures.web.dto.AuthDtos;

import java.io.IOException;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    public OAuthLoginSuccessHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User principal)) {
            response.sendRedirect("/#/signin?oauth_error=provider");
            return;
        }

        String email = principal.getAttribute("email");
        Object verifiedClaim = principal.getAttribute("email_verified");
        if (email == null || email.isBlank() || !isVerified(verifiedClaim)) {
            response.sendRedirect("/#/signin?oauth_error=unverified");
            return;
        }

        String fullName = displayName(principal, email);
        AuthDtos.AuthResponse auth = authService.loginWithProvider(email, fullName);
        request.getSession(true).setAttribute("cbd.oauth.auth-response", auth);
        response.sendRedirect("/#/oauth-complete");
    }

    private boolean isVerified(Object claim) {
        return Boolean.TRUE.equals(claim)
                || (claim != null && "true".equalsIgnoreCase(String.valueOf(claim)));
    }

    private String displayName(OAuth2User principal, String email) {
        String name = principal.getAttribute("name");
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        String givenName = principal.getAttribute("given_name");
        String familyName = principal.getAttribute("family_name");
        String combined = ((givenName == null ? "" : givenName.trim()) + " "
                + (familyName == null ? "" : familyName.trim())).trim();
        return combined.isBlank() ? email.substring(0, email.indexOf('@')) : combined;
    }
}
