package com.example.order.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderEntityTest {

    @Test
    void testOrderEntityGettersAndSetters() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setUsername("alice");
        order.setProductId(5L);
        order.setProductName("Gaming Mouse");
        order.setQuantity(3);
        order.setTotalAmount(150.0);
        order.setStatus("PAID");
        long now = System.currentTimeMillis();
        order.setCreatedAt(now);

        assertEquals(1L, order.getId());
        assertEquals("alice", order.getUsername());
        assertEquals(5L, order.getProductId());
        assertEquals("Gaming Mouse", order.getProductName());
        assertEquals(3, order.getQuantity());
        assertEquals(150.0, order.getTotalAmount());
        assertEquals("PAID", order.getStatus());
        assertEquals(now, order.getCreatedAt());
    }

    @Test
    void testOrderEntityConstructor() {
        OrderEntity order = new OrderEntity("bob", 2L, "Keyboard", 1, 80.0, "PENDING");

        assertEquals("bob", order.getUsername());
        assertEquals(2L, order.getProductId());
        assertEquals("Keyboard", order.getProductName());
        assertEquals(1, order.getQuantity());
        assertEquals(80.0, order.getTotalAmount());
        assertEquals("PENDING", order.getStatus());
        assertNotNull(order.getCreatedAt());
    }
}
