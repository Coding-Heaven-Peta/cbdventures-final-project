package za.ac.richfield.cbdventures.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OAuthClientConfiguration {

    @Bean
    @Conditional(OAuthProviderConfigured.class)
    ClientRegistrationRepository clientRegistrationRepository(Environment environment) {
        List<ClientRegistration> registrations = new ArrayList<>();
        String googleId = environment.getProperty("cbd.oauth.google.client-id", "");
        String googleSecret = environment.getProperty("cbd.oauth.google.client-secret", "");
        if (configured(googleId) && configured(googleSecret)) {
            registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(googleId.trim())
                    .clientSecret(googleSecret.trim())
                    .scope("openid", "email", "profile")
                    .clientName("Google")
                    .build());
        }

        String appleId = environment.getProperty("cbd.oauth.apple.client-id", "");
        String appleSecret = environment.getProperty("cbd.oauth.apple.client-secret", "");
        if (configured(appleId) && configured(appleSecret)) {
            registrations.add(ClientRegistration.withRegistrationId("apple")
                    .clientId(appleId.trim())
                    .clientSecret(appleSecret.trim())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "email", "name")
                    .authorizationUri("https://appleid.apple.com/auth/authorize")
                    .tokenUri("https://appleid.apple.com/auth/token")
                    .jwkSetUri("https://appleid.apple.com/auth/keys")
                    .issuerUri("https://appleid.apple.com")
                    .clientName("Apple")
                    .build());
        }

        return new InMemoryClientRegistrationRepository(registrations);
    }

    private static boolean configured(String value) {
        return value != null && !value.isBlank();
    }

    static class OAuthProviderConfigured implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            boolean google = configured(environment.getProperty("cbd.oauth.google.client-id"))
                    && configured(environment.getProperty("cbd.oauth.google.client-secret"));
            boolean apple = configured(environment.getProperty("cbd.oauth.apple.client-id"))
                    && configured(environment.getProperty("cbd.oauth.apple.client-secret"));
            return google || apple;
        }
    }
}
