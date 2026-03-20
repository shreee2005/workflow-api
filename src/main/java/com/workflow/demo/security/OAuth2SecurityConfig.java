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
                .cors(cors -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/actuator/**", "/api/debug/**", "/error").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()

                        // Existing webhook/public routes
                        .requestMatchers("/api/webhooks/**").permitAll()
                        .requestMatchers("/api/workflows/**").permitAll()
                        .requestMatchers("/hooks/**").permitAll()

                        // Protected API routes
                        .requestMatchers("/api/**").authenticated()

                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth -> oauth.successHandler(oauth2LoginSuccessHandler))
                .httpBasic(Customizer.withDefaults()); // harmless + helps actuator tooling compatibility

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(apiKeyAuthFilter, JwtAuthFilter.class);

        return http.build();
    }
}