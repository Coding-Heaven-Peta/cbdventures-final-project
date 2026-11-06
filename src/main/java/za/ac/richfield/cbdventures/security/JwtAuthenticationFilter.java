package za.ac.richfield.cbdventures.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.repo.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Reads the "Authorization: Bearer &lt;token&gt;" header, validates the JWT and
 * places the authenticated {@link User} in the security context. Requests without
 * a token simply continue anonymously, which allows public browsing and reviews
 * to remain reachable (guests may browse; only writes require a token).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            Map<String, Object> claims = jwtService.parseToken(token);
            if (claims != null) {
                Long userId = Long.valueOf(String.valueOf(claims.get("sub")));
                userRepository.findById(userId).ifPresent(user -> authenticate(user));
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(User user) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
