package com.workflow.demo.security;

import com.workflow.demo.entity.User;
import com.workflow.demo.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler
        implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            JwtUtil jwtUtil,
            @Value("${frontend.url:http://localhost:5173}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.frontendUrl = frontendUrl;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        Map<String, Object> attributes = Collections.emptyMap();

        try {
            Object principal = authentication.getPrincipal();
            if (principal != null) {
                Method getAttrs = principal.getClass().getMethod("getAttributes");
                Object attrObj = getAttrs.invoke(principal);
                if (attrObj instanceof Map<?, ?> map) {
                    attributes = (Map<String, Object>) map;
                }
            }
        } catch (Exception ignored) {}

        String email = attributes.get("email") != null
                ? attributes.get("email").toString()
                : authentication.getName();

        if (email == null || email.isBlank()) {
            response.sendError(400, "OAuth provider did not return email");
            return;
        }

        String name = attributes.get("name") != null
                ? attributes.get("name").toString()
                : email;

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setName(name);
                    u.setOauthProvider("google");
                    return userRepository.saveAndFlush(u); // 🔴 IMPORTANT
                });

        // 🔴 Ensure ID is present
        if (user.getId() == null) {
            userRepository.flush();
        }

        String jwt = jwtUtil.generateToken(
                user.getId().toString(),
                user.getEmail()
        );

        log.info("Generated JWT for user {} (email={})", user.getId(), user.getEmail());

        String redirect = UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam("token", jwt)
                .build()
                .toUriString();

        response.sendRedirect(redirect);
    }
}
