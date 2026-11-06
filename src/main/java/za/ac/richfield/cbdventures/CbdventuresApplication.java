package za.ac.richfield.cbdventures;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * CBDVentures - Pretoria CBD in Your Pocket.
 *
 * <p>RESTful backend for the CBDVentures hyper-local discovery platform
 * (IT Project - Group 5). Implements the Spring Boot layer of the architecture
 * defined in Phase 2 (System Design) and Phase 3 (Analysis / Data Modelling).</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CbdventuresApplication {

    public static void main(String[] args) {
        SpringApplication.run(CbdventuresApplication.class, args);
    }
}
