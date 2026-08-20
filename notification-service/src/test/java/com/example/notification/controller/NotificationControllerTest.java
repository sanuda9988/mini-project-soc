package com.example.notification.controller;

import com.example.notification.model.NotificationEntity;
import com.example.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationControllerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetNotificationHistory() {
        NotificationEntity n1 = new NotificationEntity("user1@example.com", "Subject 1", "Body 1", "EMAIL", "SENT");
        NotificationEntity n2 = new NotificationEntity("0771234567", "SMS ALERT", "Body 2", "SMS", "SENT");
        when(notificationRepository.findAll()).thenReturn(List.of(n1, n2));

        List<NotificationEntity> result = notificationController.getNotificationHistory();

        assertEquals(2, result.size());
        verify(notificationRepository, times(1)).findAll();
    }

    @Test
    void testGetNotificationsByRecipient() {
        NotificationEntity n1 = new NotificationEntity("user1@example.com", "Subject 1", "Body 1", "EMAIL", "SENT");
        when(notificationRepository.findByRecipient("user1@example.com")).thenReturn(List.of(n1));

        List<NotificationEntity> result = notificationController.getNotificationsByRecipient("user1@example.com");

        assertEquals(1, result.size());
        assertEquals("user1@example.com", result.get(0).getRecipient());
        verify(notificationRepository, times(1)).findByRecipient("user1@example.com");
    }

    @Test
    void testSendEmailSuccess() {
        Map<String, String> request = Map.of(
                "recipient", "user@example.com",
                "subject", "Order Confirmation",
                "body", "Your order has been placed successfully."
        );

        NotificationEntity savedEntity = new NotificationEntity("user@example.com", "Order Confirmation", "Your order has been placed successfully.", "EMAIL", "SENT");
        savedEntity.setId(1L);

        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(savedEntity);

        ResponseEntity<?> response = notificationController.sendEmail(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody() instanceof NotificationEntity);
        NotificationEntity body = (NotificationEntity) response.getBody();
        assertEquals("user@example.com", body.getRecipient());
        assertEquals("EMAIL", body.getType());
        assertEquals("SENT", body.getStatus());
    }

    @Test
    void testSendEmailMissingFields() {
        Map<String, String> request = Map.of(
                "recipient", "user@example.com"
        );

        ResponseEntity<?> response = notificationController.sendEmail(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testSendSmsSuccess() {
        Map<String, String> request = Map.of(
                "recipient", "+94771234567",
                "body", "Your verification code is 123456"
        );

        NotificationEntity savedEntity = new NotificationEntity("+94771234567", "SMS ALERT", "Your verification code is 123456", "SMS", "SENT");
        savedEntity.setId(2L);

        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(savedEntity);

        ResponseEntity<?> response = notificationController.sendSMS(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody() instanceof NotificationEntity);
        NotificationEntity body = (NotificationEntity) response.getBody();
        assertEquals("+94771234567", body.getRecipient());
        assertEquals("SMS", body.getType());
        assertEquals("SENT", body.getStatus());
    }

    @Test
    void testSendSmsMissingFields() {
        Map<String, String> request = Map.of(
                "recipient", "+94771234567"
        );

        ResponseEntity<?> response = notificationController.sendSMS(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
