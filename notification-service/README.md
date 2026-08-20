# 🔔 Notification Service

The **Notification Service** handles alert dispatching, email notifications, and SMS messages across the microservices platform.

---

## 📌 Details

| Parameter | Details |
| :--- | :--- |
| **Service Name** | `notification-service` |
| **Port** | `8085` |
| **API Key Header** | `X-API-KEY: notification-service-api-key-secret` |
| **Database** | In-Memory H2 Database (`jdbc:h2:mem:notificationdb`) |
| **Swagger UI** | `http://localhost:8085/swagger-ui.html` |
| **H2 Console** | `http://localhost:8085/h2-console` |

---

## ⚡ Key Endpoints

| Method | Endpoint | Description | Header Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/notify/history` | Fetch all notification history | `X-API-KEY: notification-service-api-key-secret` |
| `GET` | `/notify/recipient?email={email}` | Fetch notifications by recipient | `X-API-KEY: notification-service-api-key-secret` |
| `POST` | `/notify/email` | Send email notification | `X-API-KEY: notification-service-api-key-secret` |
| `POST` | `/notify/sms` | Send SMS notification | `X-API-KEY: notification-service-api-key-secret` |

---

## 🚀 How to Run

### 1. Run using Maven
```bash
mvn spring-boot:run
```

### 2. Run Tests
```bash
mvn test
```

### 3. Docker Container
```bash
docker build -t notification-service .
docker run -p 8085:8085 notification-service
```

---

## 🧪 Example API Requests

### Send Email Notification
```bash
curl -X POST http://localhost:8085/notify/email \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: notification-service-api-key-secret" \
  -d '{
    "recipient": "user@example.com",
    "subject": "Order Confirmation",
    "body": "Your order #100 has been placed successfully."
  }'
```

### Send SMS Notification
```bash
curl -X POST http://localhost:8085/notify/sms \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: notification-service-api-key-secret" \
  -d '{
    "recipient": "+94771234567",
    "body": "Your order #100 has been confirmed."
  }'
```

### Fetch Notification History
```bash
curl -X GET http://localhost:8085/notify/history \
  -H "X-API-KEY: notification-service-api-key-secret"
```
