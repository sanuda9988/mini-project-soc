package com.example.gateway.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class RateLimiterFilter implements Filter {

    @Value("${gateway.rate.limit.capacity}")
    private double capacity;

    @Value("${gateway.rate.limit.refill.rate}")
    private double refillRate;

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private static class TokenBucket {
        double tokens;
        long lastRefilled;

        TokenBucket(double capacity) {
            this.tokens = capacity;
            this.lastRefilled = System.currentTimeMillis();
        }

        synchronized boolean tryConsume(double capacity, double refillRate) {
            long now = System.currentTimeMillis();
            double elapsedSeconds = (now - lastRefilled) / 1000.0;
            lastRefilled = now;

            tokens = Math.min(capacity, tokens + (elapsedSeconds * refillRate));

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = httpRequest.getRemoteAddr();
        TokenBucket bucket = buckets.computeIfAbsent(clientIp, k -> new TokenBucket(capacity));

        if (!bucket.tryConsume(capacity, refillRate)) {
            httpResponse.setStatus(429); // Too Many Requests
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Please try again in a few seconds.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
