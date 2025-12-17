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
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // read frontend URL from properties (application-dev.properties has frontend.url=http://localhost:5173)
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(UserRepository userRepository,
                                     JwtUtil jwtUtil,
                                     @Value("${frontend.url:http://localhost:5174}") String frontendUrl) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.frontendUrl = frontendUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            Object principal = authentication.getPrincipal();
            Map<String, Object> attributes = Collections.emptyMap();

            // If principal implements getAttributes (typical OAuth2User / OidcUser), use it.
            if (principal != null) {
                try {
                    Method getAttrs = principal.getClass().getMethod("getAttributes");
                    Object attrObj = getAttrs.invoke(principal);
                    if (attrObj instanceof Map) {
                        attributes = (Map<String, Object>) attrObj;
                    }
                } catch (NoSuchMethodException nsme) {
                    // Not an OAuth2User type; fallback later
                } catch (Exception ex) {
                    log.warn("Failed to read attributes from principal via reflection", ex);
                }
            }

            String provider = "google";
            String providerId = null;
            String email = null;
            String name = null;

            if (attributes != null && !attributes.isEmpty()) {
                Object sub = attributes.get("sub");
                if (sub == null) sub = attributes.get("id");
                if (sub != null) providerId = String.valueOf(sub);

                Object mail = attributes.get("email");
                if (mail != null) email = String.valueOf(mail);

                Object nm = attributes.get("name");
                if (nm != null) name = String.valueOf(nm);
            }

            // fallback to authentication.getName() if no email found
            if ((email == null || email.isBlank()) && authentication.getName() != null) {
                email = authentication.getName();
            }

            if ((name == null || name.isBlank()) && email != null) {
                name = email;
            }

            if (email == null || email.isBlank()) {
                log.error("OAuth login succeeded but no usable email found. principalClass={}",
                        principal == null ? "null" : principal.getClass().getName());
                sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "oauth_no_email", "No email available from provider");
                return;
            }

            // Link or create user
            Optional<User> maybe = userRepository.findByEmail(email);
            User user;
            if (maybe.isPresent()) {
                user = maybe.get();
                boolean changed = false;
                if (user.getOauthProvider() == null) {
                    user.setOauthProvider(provider);
                    changed = true;
                }
                if (user.getOauthId() == null && providerId != null) {
                    user.setOauthId(providerId);
                    changed = true;
                }
                if (changed) userRepository.save(user);
            } else {
                user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setOauthProvider(provider);
                if (providerId != null) user.setOauthId(providerId);
                // password left null (we rely on OAuth)
                userRepository.save(user);
            }

            // create app JWT using the same JwtUtil
            String jwt = jwtUtil.generateToken(user.getId().toString(), user.getEmail());

            // DEBUG: if you still want to inspect token
            // System.out.println("=== GENERATED JWT ===");
            // System.out.println(jwt);
            // System.out.println("======================");

            // Build redirect to frontend (from application properties you set frontend.url). Default port 5173.
            String redirect = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/")
                    .queryParam("token", jwt)
                    .build().toUriString();

            log.info("Generated JWT for user {} (email={}) and redirecting to frontend.",
                    user.getId(), user.getEmail());

            // send redirect to UI where your JS will read token query param
            response.sendRedirect(redirect);
        } catch (Exception ex) {
            log.error("OAuth2 success-handler failed", ex);
            sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "oauth_success_failed", ex.getMessage());
        }
    }

    private void sendJsonError(HttpServletResponse response,
                               int status,
                               String code,
                               String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        String safeMsg = message == null ? "" : message.replace("\"", "\\\"");
        String body = String.format("{\"error\":\"%s\",\"message\":\"%s\"}", code, safeMsg);
        response.getWriter().write(body);
    }
}
