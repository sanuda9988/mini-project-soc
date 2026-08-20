package com.example.notification.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationEntityTest {

    @Test
    void testNotificationEntityGettersAndSetters() {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(1L);
        notification.setRecipient("john@example.com");
        notification.setSubject("Order Confirmed");
        notification.setBody("Your order has been placed.");
        notification.setType("EMAIL");
        notification.setStatus("SENT");
        long now = System.currentTimeMillis();
        notification.setSentAt(now);

        assertEquals(1L, notification.getId());
        assertEquals("john@example.com", notification.getRecipient());
        assertEquals("Order Confirmed", notification.getSubject());
        assertEquals("Your order has been placed.", notification.getBody());
        assertEquals("EMAIL", notification.getType());
        assertEquals("SENT", notification.getStatus());
        assertEquals(now, notification.getSentAt());
    }

    @Test
    void testNotificationEntityConstructor() {
        NotificationEntity notification = new NotificationEntity("jane@example.com", "Alert", "Test Message", "EMAIL", "SENT");

        assertEquals("jane@example.com", notification.getRecipient());
        assertEquals("Alert", notification.getSubject());
        assertEquals("Test Message", notification.getBody());
        assertEquals("EMAIL", notification.getType());
        assertEquals("SENT", notification.getStatus());
        assertNotNull(notification.getSentAt());
    }
}
