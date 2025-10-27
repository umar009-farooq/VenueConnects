package com.venueconnect.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // --- START OF UPDATED SECTION ---
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api-docs",          // <-- CORRECTED (was /v3/api-docs)
                                "/api-docs/**",       // <-- CORRECTED (was /v3/api-docs/**)
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        )
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/**")
                                .permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/venues", "/api/v1/venues/**")
                                .permitAll()
                        // Secure reservation/order endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/reservations/**")
                        .hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/**")
                        .hasAnyRole("USER", "ADMIN")
                        // --- ADD CANCELLATION RULE ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/{id}/cancel") // Specific cancel endpoint
                        .hasAnyRole("USER", "ADMIN", "ORGANIZER") // Allow users, admins, organizers
                        // --- END CANCELLATION RULE ---

                        // Admin endpoints
                        .requestMatchers("/api/v1/admin/**")
                        .hasAnyRole("ADMIN", "ORGANIZER")

                        // All other endpoints require authentication
                        .anyRequest()
                        .authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}