package za.ac.richfield.cbdventures.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class OAuthProviderSettings {

    private final Environment environment;

    public OAuthProviderSettings(Environment environment) {
        this.environment = environment;
    }

    public boolean googleEnabled() {
        return configured("cbd.oauth.google.client-id")
                && configured("cbd.oauth.google.client-secret");
    }

    public boolean appleEnabled() {
        return configured("cbd.oauth.apple.client-id")
                && configured("cbd.oauth.apple.client-secret");
    }

    private boolean configured(String key) {
        String value = environment.getProperty(key);
        return value != null && !value.isBlank();
    }
}
