package com.example.order.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;

import static org.mockito.Mockito.*;

class ApiKeyFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @InjectMocks
    private ApiKeyFilter apiKeyFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(apiKeyFilter, "configuredApiKey", "order-service-api-key-secret");
    }

    @Test
    void testFilterAllowsSwaggerPath() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/swagger-ui.html");

        apiKeyFilter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void testFilterAllowsH2ConsolePath() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/h2-console/index.html");

        apiKeyFilter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void testFilterAllowsValidApiKey() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/orders");
        when(request.getHeader("X-API-KEY")).thenReturn("order-service-api-key-secret");

        apiKeyFilter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void testFilterRejectsInvalidApiKey() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/orders");
        when(request.getHeader("X-API-KEY")).thenReturn("wrong-key");
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        apiKeyFilter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(request, response);
    }
}
