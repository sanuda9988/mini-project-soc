package com.example.gateway.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@Component
@Order(2)
public class JwtAuthFilter implements Filter {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // 1. Define public routes
        boolean isPublic = true;

        if (path.startsWith("/orders") || path.startsWith("/payments") || path.startsWith("/notify")) {
            isPublic = false;
        }

        // Allow Swagger & H2 docs on Gateway to be public
        if (path.contains("/swagger-ui") || path.contains("/api-docs") || path.contains("/v3/api-docs")) {
            isPublic = true;
        }

        if (isPublic) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Extract Bearer Token
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(httpResponse, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header. Expected 'Bearer <JWT>'");
            return;
        }

        String token = authHeader.substring(7);

        // 3. Contact auth-service to validate token
        try {
            ResponseEntity<Map> authResp = restTemplate.getForEntity(
                    authServiceUrl + "/auth/validate?token=" + token,
                    Map.class
            );

            if (authResp.getStatusCode() == HttpStatus.OK && authResp.getBody() != null) {
                Map<String, Object> body = authResp.getBody();
                if (Boolean.TRUE.equals(body.get("valid"))) {
                    // Inject claims as headers so routing controller can access them and pass to downstream
                    httpRequest.setAttribute("username", body.get("username").toString());
                    httpRequest.setAttribute("role", body.get("role").toString());
                    
                    chain.doFilter(request, response);
                    return;
                }
            }
        } catch (Exception e) {
            // Service call failed or returned error
        }

        sendError(httpResponse, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"%s\", \"message\": \"%s\"}", status.getReasonPhrase(), message));
    }
}
