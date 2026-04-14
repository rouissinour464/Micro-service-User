package com.pfe.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ActuatorSecurityConfig {

    @Bean
    @Order(0) // ✅ PRIORITÉ MAXIMALE
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {

        http
            // ✅ Cette chaîne ne s'applique QU'A ACTUATOR
            .securityMatcher("/actuator/**")

            // ✅ TOUT AUTORISER
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

            // ✅ Pas de CSRF pour actuator
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
