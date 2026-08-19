package com.example.order.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiKeyFilter implements Filter {

    @Value("${api.key}")
    private String configuredApiKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Exclude Swagger, API Docs, and H2 Console
        if (path.contains("/swagger-ui") || 
            path.contains("/api-docs") || 
            path.contains("/v3/api-docs") || 
            path.contains("/h2-console")) {
            chain.doFilter(request, response);
            return;
        }

        String requestApiKey = httpRequest.getHeader("X-API-KEY");

        if (requestApiKey == null || !requestApiKey.equals(configuredApiKey)) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Direct microservice access requires a valid API Key.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
