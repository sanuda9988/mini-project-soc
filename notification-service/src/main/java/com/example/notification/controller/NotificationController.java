package com.example.notification.controller;

import com.example.notification.model.NotificationEntity;
import com.example.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notify")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/history")
    public List<NotificationEntity> getNotificationHistory() {
        return notificationRepository.findAll();
    }

    @GetMapping("/recipient")
    public List<NotificationEntity> getNotificationsByRecipient(@RequestParam("email") String email) {
        return notificationRepository.findByRecipient(email);
    }

    @PostMapping("/email")
    public ResponseEntity<?> sendEmail(@RequestBody Map<String, String> request) {
        String recipient = request.get("recipient");
        String subject = request.get("subject");
        String body = request.get("body");

        if (recipient == null || subject == null || body == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Recipient, subject, and body are required"));
        }

        NotificationEntity notification = new NotificationEntity(recipient, subject, body, "EMAIL", "SENT");
        notification = notificationRepository.save(notification);

        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }

    @PostMapping("/sms")
    public ResponseEntity<?> sendSMS(@RequestBody Map<String, String> request) {
        String recipient = request.get("recipient"); // Phone number
        String body = request.get("body");

        if (recipient == null || body == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Recipient phone number and body are required"));
        }

        NotificationEntity notification = new NotificationEntity(recipient, "SMS ALERT", body, "SMS", "SENT");
        notification = notificationRepository.save(notification);

        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }
}
