package com.acme.catchup.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main entry point for the CatchUp Platform Spring Boot application.
 * Enables JPA auditing support for auditing entity fields.
 *
 * @since 1.0
 */
@EnableJpaAuditing
@SpringBootApplication
public class CatchUpPlatformApplication {

    static void main(String[] args) {
        SpringApplication.run(CatchUpPlatformApplication.class, args);
    }

}