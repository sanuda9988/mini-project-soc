package com.example.order.controller;

import com.example.order.model.OrderEntity;
import com.example.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${product.service.api.key}")
    private String productServiceApiKey;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Value("${payment.service.api.key}")
    private String paymentServiceApiKey;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    @Value("${notification.service.api.key}")
    private String notificationServiceApiKey;

    @GetMapping
    public List<OrderEntity> getAllOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/user/{username}")
    public List<OrderEntity> getOrdersByUsername(@PathVariable String username) {
        return orderRepository.findByUsername(username);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        Optional<OrderEntity> orderOpt = orderRepository.findById(id);
        if (orderOpt.isPresent()) {
            return ResponseEntity.ok(orderOpt.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Order not found"));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody Map<String, Object> request) {
        if (request == null || !request.containsKey("username") || !request.containsKey("productId") || !request.containsKey("quantity")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid request parameters: username, productId, and quantity are required."));
        }

        String username = request.get("username").toString();
        Long productId;
        Integer quantity;
        try {
            productId = Long.valueOf(request.get("productId").toString());
            quantity = Integer.valueOf(request.get("quantity").toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "productId and quantity must be valid numbers."));
        }

        if (quantity <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "quantity must be greater than 0."));
        }

        String cardNumber = request.getOrDefault("cardNumber", "4000-1234-5678-9010").toString();

        // 1. Fetch product details from product-service
        HttpHeaders productHeaders = new HttpHeaders();
        productHeaders.set("X-API-KEY", productServiceApiKey);
        HttpEntity<Void> productEntity = new HttpEntity<>(productHeaders);

        Map<String, Object> product = null;
        try {
            ResponseEntity<Map> productResp = restTemplate.exchange(
                    productServiceUrl + "/products/" + productId,
                    HttpMethod.GET,
                    productEntity,
                    Map.class
            );
            product = productResp.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Product not found"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to contact product-service"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Product service unavailable: " + e.getMessage()));
        }

        String productName = product.get("name").toString();
        Double price = Double.valueOf(product.get("price").toString());
        Double totalAmount = price * quantity;

        // 2. Reduce stock in product-service
        HttpHeaders reduceStockHeaders = new HttpHeaders();
        reduceStockHeaders.set("X-API-KEY", productServiceApiKey);
        reduceStockHeaders.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> reduceStockBody = Map.of("productId", productId, "quantity", quantity);
        HttpEntity<Map> reduceStockEntity = new HttpEntity<>(reduceStockBody, reduceStockHeaders);

        try {
            restTemplate.postForEntity(
                    productServiceUrl + "/products/reduce-stock",
                    reduceStockEntity,
                    Map.class
            );
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Insufficient stock or error reducing stock: " + e.getResponseBodyAsString()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to update product stock: " + e.getMessage()));
        }

        // 3. Create Order in PENDING status
        OrderEntity order = new OrderEntity(username, productId, productName, quantity, totalAmount, "PENDING");
        order = orderRepository.save(order);

        // 4. Process payment via payment-service
        HttpHeaders paymentHeaders = new HttpHeaders();
        paymentHeaders.set("X-API-KEY", paymentServiceApiKey);
        paymentHeaders.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> paymentBody = Map.of(
                "orderId", order.getId(),
                "amount", totalAmount,
                "cardNumber", cardNumber
        );
        HttpEntity<Map> paymentEntity = new HttpEntity<>(paymentBody, paymentHeaders);

        String paymentStatus = "FAILED";
        String transactionId = "N/A";
        try {
            ResponseEntity<Map> paymentResp = restTemplate.postForEntity(
                    paymentServiceUrl + "/payments/process",
                    paymentEntity,
                    Map.class
            );
            Map<String, Object> paymentResult = paymentResp.getBody();
            if (paymentResult != null && "SUCCESS".equals(paymentResult.get("status"))) {
                paymentStatus = "PAID";
                transactionId = paymentResult.get("transactionId").toString();
            }
        } catch (Exception e) {
            // Log error and treat as failed
            System.err.println("Payment service failed: " + e.getMessage());
        }

        // Update order status
        order.setStatus(paymentStatus);
        order = orderRepository.save(order);

        // 5. Send notification via notification-service
        HttpHeaders notifyHeaders = new HttpHeaders();
        notifyHeaders.set("X-API-KEY", notificationServiceApiKey);
        notifyHeaders.setContentType(MediaType.APPLICATION_JSON);
        
        String emailBody = String.format(
                "Hello %s,\n\nYour order #%d for %dx %s has been processed.\nStatus: %s\nTotal: $%s\nTransaction ID: %s\n\nThank you for shopping with us!",
                username, order.getId(), quantity, productName, paymentStatus, totalAmount, transactionId
        );
        
        Map<String, Object> notifyBody = Map.of(
                "recipient", username + "@example.com",
                "subject", "Order Status Update #" + order.getId(),
                "body", emailBody
        );
        HttpEntity<Map> notifyEntity = new HttpEntity<>(notifyBody, notifyHeaders);

        String notificationStatus = "SENT";
        try {
            restTemplate.postForEntity(
                    notificationServiceUrl + "/notify/email",
                    notifyEntity,
                    Map.class
            );
        } catch (Exception e) {
            notificationStatus = "FAILED_TO_SEND";
            System.err.println("Notification service failed: " + e.getMessage());
        }

        // Return combined response
        Map<String, Object> response = new HashMap<>();
        response.put("order", order);
        response.put("paymentStatus", paymentStatus);
        response.put("transactionId", transactionId);
        response.put("notificationStatus", notificationStatus);

        return ResponseEntity.ok(response);
    }
}
