package com.workflow.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final String internalApiKey;

    public ApiKeyAuthFilter() {
        this.internalApiKey = System.getenv("INTERNAL_API_KEY");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // For actuator paths, bypass filter logic fully
        if (shouldNotFilter(request)) {
            chain.doFilter(request, response);
            return;
        }

        if (internalApiKey != null && !internalApiKey.isBlank()) {
            String headerKey = request.getHeader("X-Internal-API-Key");
            if (headerKey != null && headerKey.equals(internalApiKey)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        "internal-worker",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}