package com.example.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Enumeration;
import java.util.Map;

/**
 * API Gateway Routing Controller
 *
 * Acts as a reverse proxy — routes requests from clients to the appropriate
 * downstream microservice, injecting the required X-API-KEY header for
 * service-to-service authentication.
 *
 * JWT validation is performed upstream in JwtAuthFilter before reaching here.
 */
@RestController
public class GatewayRoutingController {

    @Autowired
    private RestTemplate restTemplate;

    // ─── Downstream Service URLs ───────────────────────────────────────────────

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${order.service.url}")
    private String orderServiceUrl;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    // ─── API Keys ──────────────────────────────────────────────────────────────

    @Value("${product.service.api.key}")
    private String productApiKey;

    @Value("${order.service.api.key}")
    private String orderApiKey;

    @Value("${payment.service.api.key}")
    private String paymentApiKey;

    @Value("${notification.service.api.key}")
    private String notificationApiKey;

    // ─── Auth Routes (public, no API key needed) ───────────────────────────────

    @RequestMapping(value = "/auth/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyAuth(HttpServletRequest request, @RequestBody(required = false) String body) {
        String downstreamPath = request.getRequestURI();
        String downstreamUrl = authServiceUrl + downstreamPath;
        if (request.getQueryString() != null) {
            downstreamUrl += "?" + request.getQueryString();
        }
        return forward(request, body, downstreamUrl, null);
    }

    // ─── Product Routes (JWT protected) ───────────────────────────────────────

    @RequestMapping(value = "/products/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyProducts(HttpServletRequest request, @RequestBody(required = false) String body) {
        String downstreamPath = request.getRequestURI();
        String downstreamUrl = productServiceUrl + downstreamPath;
        if (request.getQueryString() != null) {
            downstreamUrl += "?" + request.getQueryString();
        }
        return forward(request, body, downstreamUrl, productApiKey);
    }

    // ─── Order Routes (JWT protected) ─────────────────────────────────────────

    @RequestMapping(value = "/orders/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyOrders(HttpServletRequest request, @RequestBody(required = false) String body) {
        String downstreamPath = request.getRequestURI();
        String downstreamUrl = orderServiceUrl + downstreamPath;
        if (request.getQueryString() != null) {
            downstreamUrl += "?" + request.getQueryString();
        }
        return forward(request, body, downstreamUrl, orderApiKey);
    }

    // ─── Payment Routes (JWT protected) ───────────────────────────────────────

    @RequestMapping(value = "/payments/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyPayments(HttpServletRequest request, @RequestBody(required = false) String body) {
        String downstreamPath = request.getRequestURI();
        String downstreamUrl = paymentServiceUrl + downstreamPath;
        if (request.getQueryString() != null) {
            downstreamUrl += "?" + request.getQueryString();
        }
        return forward(request, body, downstreamUrl, paymentApiKey);
    }

    // ─── Notification Routes (JWT protected) ──────────────────────────────────

    @RequestMapping(value = "/notify/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyNotifications(HttpServletRequest request, @RequestBody(required = false) String body) {
        String downstreamPath = request.getRequestURI();
        String downstreamUrl = notificationServiceUrl + downstreamPath;
        if (request.getQueryString() != null) {
            downstreamUrl += "?" + request.getQueryString();
        }
        return forward(request, body, downstreamUrl, notificationApiKey);
    }

    // ─── Health Check ──────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "service", "api-gateway",
                "status", "UP",
                "timestamp", System.currentTimeMillis()
        ));
    }

    // ─── Helper: Forward request to downstream service ─────────────────────────

    private ResponseEntity<?> forward(HttpServletRequest request, String body, String downstreamUrl, String apiKey) {
        try {
            HttpHeaders headers = new HttpHeaders();

            // Copy over original request headers (except host)
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("content-length")) {
                        headers.set(name, request.getHeader(name));
                    }
                }
            }

            // Inject API key for downstream service auth
            if (apiKey != null) {
                headers.set("X-API-KEY", apiKey);
            }

            // Propagate JWT-derived user context (set as attributes by JwtAuthFilter)
            Object username = request.getAttribute("username");
            Object role = request.getAttribute("role");
            if (username != null) headers.set("X-Username", username.toString());
            if (role != null) headers.set("X-Role", role.toString());

            // Set content type for body requests
            if (body != null && !body.isEmpty()) {
                if (headers.getContentType() == null) {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                }
            }

            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            return restTemplate.exchange(downstreamUrl, method, entity, String.class);

        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(String.format("{\"error\":\"Bad Gateway\",\"message\":\"Could not connect to downstream service: %s\"}", e.getMessage()));
        }
    }
}
