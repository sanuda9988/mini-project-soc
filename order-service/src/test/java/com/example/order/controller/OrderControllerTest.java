package com.example.order.controller;

import com.example.order.model.OrderEntity;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OrderControllerTest {

    @Mock
    private OrderRepository orderRepository;

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private OrderController orderController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        orderController = new OrderController();
        ReflectionTestUtils.setField(orderController, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(orderController, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(orderController, "productServiceUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(orderController, "productServiceApiKey", "product-service-api-key-secret");
        ReflectionTestUtils.setField(orderController, "paymentServiceUrl", "http://localhost:8084");
        ReflectionTestUtils.setField(orderController, "paymentServiceApiKey", "payment-service-api-key-secret");
        ReflectionTestUtils.setField(orderController, "notificationServiceUrl", "http://localhost:8085");
        ReflectionTestUtils.setField(orderController, "notificationServiceApiKey", "notification-service-api-key-secret");
    }

    @Test
    void testGetAllOrders() {
        OrderEntity order1 = new OrderEntity("user1", 1L, "Laptop", 1, 1000.0, "PAID");
        OrderEntity order2 = new OrderEntity("user2", 2L, "Mouse", 2, 50.0, "PAID");
        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

        List<OrderEntity> orders = orderController.getAllOrders();

        assertEquals(2, orders.size());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void testGetOrdersByUsername() {
        OrderEntity order = new OrderEntity("john", 1L, "Laptop", 1, 1000.0, "PAID");
        when(orderRepository.findByUsername("john")).thenReturn(List.of(order));

        List<OrderEntity> orders = orderController.getOrdersByUsername("john");

        assertEquals(1, orders.size());
        assertEquals("john", orders.get(0).getUsername());
        verify(orderRepository, times(1)).findByUsername("john");
    }

    @Test
    void testGetOrderByIdSuccess() {
        OrderEntity order = new OrderEntity("john", 1L, "Laptop", 1, 1000.0, "PAID");
        order.setId(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        ResponseEntity<?> response = orderController.getOrderById(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(order, response.getBody());
    }

    @Test
    void testGetOrderByIdNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = orderController.getOrderById(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testCheckoutSuccess() {
        Map<String, Object> request = new HashMap<>();
        request.put("username", "john_doe");
        request.put("productId", 1);
        request.put("quantity", 2);
        request.put("cardNumber", "4000-1234-5678-9010");

        // 1. Mock GET product
        mockServer.expect(requestTo("http://localhost:8082/products/1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "product-service-api-key-secret"))
                .andRespond(withSuccess("{\"id\":1, \"name\":\"Wireless Headphones\", \"price\":150.0}", MediaType.APPLICATION_JSON));

        // 2. Mock POST reduce stock
        mockServer.expect(requestTo("http://localhost:8082/products/reduce-stock"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", "product-service-api-key-secret"))
                .andRespond(withSuccess("{\"message\":\"Stock reduced\"}", MediaType.APPLICATION_JSON));

        // Mock repository save
        OrderEntity pendingOrder = new OrderEntity("john_doe", 1L, "Wireless Headphones", 2, 300.0, "PENDING");
        pendingOrder.setId(100L);
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(pendingOrder);

        // 3. Mock POST payment
        mockServer.expect(requestTo("http://localhost:8084/payments/process"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", "payment-service-api-key-secret"))
                .andRespond(withSuccess("{\"status\":\"SUCCESS\", \"transactionId\":\"TXN12345\"}", MediaType.APPLICATION_JSON));

        // 4. Mock POST notification
        mockServer.expect(requestTo("http://localhost:8085/notify/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", "notification-service-api-key-secret"))
                .andRespond(withSuccess("{\"status\":\"SENT\"}", MediaType.APPLICATION_JSON));

        ResponseEntity<?> response = orderController.checkout(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> respMap = (Map<?, ?>) response.getBody();
        assertEquals("PAID", respMap.get("paymentStatus"));
        assertEquals("TXN12345", respMap.get("transactionId"));
        assertEquals("SENT", respMap.get("notificationStatus"));

        mockServer.verify();
    }

    @Test
    void testCheckoutProductNotFound() {
        Map<String, Object> request = Map.of("username", "john_doe", "productId", 99, "quantity", 1);

        mockServer.expect(requestTo("http://localhost:8082/products/99"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ResponseEntity<?> response = orderController.checkout(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        mockServer.verify();
    }

    @Test
    void testCheckoutInvalidInput() {
        Map<String, Object> request = Map.of("username", "john_doe");

        ResponseEntity<?> response = orderController.checkout(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
