package com.pfe.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;

@Configuration
public class ActuatorSecurityConfig {

    @Bean
    @Order(0) // ✅ priorité MAX
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {

        http
            // ✅ MATCH ACTUATOR (OFFICIEL)
            .securityMatcher(EndpointRequest.toAnyEndpoint())

            // ✅ TOUT AUTORISER
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

            // ✅ PAS DE CSRF
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
