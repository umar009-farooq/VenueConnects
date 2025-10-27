package com.venueconnect.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    /**
     * This bean will run ONLY when the 'dev' profile is active.
     * It cleans the database and then migrates it on startup.
     * Perfect for a clean slate during development and testing.
     */
    @Bean
    @Profile("dev")
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .cleanDisabled(false) // Allow cleaning
                .load();
        flyway.clean(); // WIPE the database
        flyway.migrate(); // REBUILD the database from V1, V2, etc.
        return flyway;
    }
}