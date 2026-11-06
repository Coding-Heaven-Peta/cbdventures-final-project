package za.ac.richfield.cbdventures.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import za.ac.richfield.cbdventures.security.JwtAuthenticationFilter;
import za.ac.richfield.cbdventures.security.OAuthLoginFailureHandler;
import za.ac.richfield.cbdventures.security.OAuthLoginSuccessHandler;

import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectProvider<ClientRegistrationRepository> oauthClients;
    private final OAuthLoginSuccessHandler oauthSuccessHandler;
    private final OAuthLoginFailureHandler oauthFailureHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ObjectProvider<ClientRegistrationRepository> oauthClients,
                          OAuthLoginSuccessHandler oauthSuccessHandler,
                          OAuthLoginFailureHandler oauthFailureHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oauthClients = oauthClients;
        this.oauthSuccessHandler = oauthSuccessHandler;
        this.oauthFailureHandler = oauthFailureHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean oauthConfigured = oauthClients.getIfAvailable() != null;
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults());

        http.sessionManagement(session -> session.sessionCreationPolicy(oauthConfigured
                ? SessionCreationPolicy.IF_REQUIRED : SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**",
                                "/icons/**", "/manifest.webmanifest", "/sw.js", "/favicon.ico").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login",
                                "/api/auth/verify", "/api/auth/resend", "/api/auth/oauth/exchange").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/auth/me",
                                "/api/auth/providers",
                                "/api/meta/**",
                                "/api/categories/**",
                                "/api/amenities/**",
                                "/api/businesses/**",
                                "/api/events/**",
                                "/api/recommendations/**",
                                "/api/discover/**").permitAll()
                        .requestMatchers("/api/assistant/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/owner/businesses").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/api/owner/**").hasAnyRole("OWNER", "ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthorisedEntryPoint())
                        .accessDeniedHandler(forbiddenHandler()))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        if (oauthConfigured) {
            http.oauth2Login(oauth -> oauth
                    .successHandler(oauthSuccessHandler)
                    .failureHandler(oauthFailureHandler));
        }

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private AuthenticationEntryPoint unauthorisedEntryPoint() {
        return (request, response, ex) -> writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized", "Please sign in to continue.");
    }

    private AccessDeniedHandler forbiddenHandler() {
        return (request, response, ex) -> writeError(response, HttpServletResponse.SC_FORBIDDEN,
                "Forbidden", "Your account does not have access to this action.");
    }

    private void writeError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                status, error, message));
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
