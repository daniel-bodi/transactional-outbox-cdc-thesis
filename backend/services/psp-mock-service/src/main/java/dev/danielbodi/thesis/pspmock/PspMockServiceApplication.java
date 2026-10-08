package dev.danielbodi.thesis.pspmock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * @author danielbodi
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PspMockServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PspMockServiceApplication.class, args);
    }
}
