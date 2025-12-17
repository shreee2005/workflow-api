package com.workflow.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class OAuth2SecurityConfig {

    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;
    private final JwtAuthFilter jwtAuthFilter;
    private final ApiKeyAuthFilter apiKeyAuthFilter;

    public OAuth2SecurityConfig(OAuth2LoginSuccessHandler oauth2LoginSuccessHandler,
                                JwtAuthFilter jwtAuthFilter,
                                ApiKeyAuthFilter apiKeyAuthFilter) {
        this.oauth2LoginSuccessHandler = oauth2LoginSuccessHandler;
        this.jwtAuthFilter = jwtAuthFilter;
        this.apiKeyAuthFilter = apiKeyAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // allow health/debug/open endpoints
                        .requestMatchers("/actuator/**", "/api/debug/**", "/error").permitAll()
                        // OAuth2 entry points
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        // everything else under /api/ requires authentication
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .successHandler(oauth2LoginSuccessHandler)
                );

        // JWT for bearer tokens
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        // API key for internal worker calls
        http.addFilterBefore(apiKeyAuthFilter, JwtAuthFilter.class);

        return http.build();
    }
}
