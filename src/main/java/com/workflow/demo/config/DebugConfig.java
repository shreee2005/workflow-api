package com.workflow.demo.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
public class DebugConfig {

    @Bean
    public ApplicationRunner runner(
            ObjectProvider<ClientRegistrationRepository> provider) {

        return args -> {
            System.out.println("==================================");
            System.out.println("OAuth Bean Present = " +
                    (provider.getIfAvailable() != null));
            System.out.println("==================================");
        };
    }

    @Value("${spring.security.oauth2.client.registration.google.client-id:NOT_FOUND}")
    private String clientId;

    @Bean
    ApplicationRunner runner2() {
        return args -> {
            System.out.println("Google Client ID = " + clientId);
        };
    }
}
