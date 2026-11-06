package za.ac.richfield.cbdventures.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the single page application shell at the site root.
 *
 * <p>The web client is a static, hash-routed SPA (index.html + /css + /js), so a
 * request for "/" is forwarded to index.html. Deep links such as
 * "/#/business/7" are handled on the client and never reach the server.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/index").setViewName("forward:/index.html");
    }
}
