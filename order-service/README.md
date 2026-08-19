# 🛒 Order Service (Student 3 - Order Service Lead)

The **Order Service** handles order creation, checkout flows, and order querying. It integrates with downstream services (`product-service`, `payment-service`, and `notification-service`).

---

## 📌 Assigned Role & Details

| Parameter | Details |
| :--- | :--- |
| **Student** | **Student 3** (Order Service Lead) |
| **Service Name** | `order-service` |
| **Port** | `8083` |
| **API Key Header** | `X-API-KEY: order-service-api-key-secret` |
| **Database** | In-Memory H2 Database (`jdbc:h2:mem:orderdb`) |
| **Swagger UI** | `http://localhost:8083/swagger-ui.html` |
| **H2 Console** | `http://localhost:8083/h2-console` |

---

## ⚡ Key Endpoints

| Method | Endpoint | Description | Header Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/orders` | Fetch all orders | `X-API-KEY: order-service-api-key-secret` |
| `GET` | `/orders/user/{username}` | Fetch orders by username | `X-API-KEY: order-service-api-key-secret` |
| `GET` | `/orders/{id}` | Fetch order details by ID | `X-API-KEY: order-service-api-key-secret` |
| `POST` | `/orders/checkout` | Process order checkout | `X-API-KEY: order-service-api-key-secret` |

---

## 🔄 Checkout Flow (`POST /orders/checkout`)

When a client calls `POST /orders/checkout`, the service executes the following steps:
1. **Validates input parameters**: checks `username`, `productId`, and `quantity` (> 0).
2. **Product Check**: Calls `GET product-service:8082/products/{productId}` to retrieve item details.
3. **Stock Reduction**: Calls `POST product-service:8082/products/reduce-stock`.
4. **Order Creation**: Saves the order in `PENDING` status.
5. **Payment Processing**: Calls `POST payment-service:8084/payments/process`.
6. **Status Update**: Updates order status to `PAID` or `FAILED`.
7. **Notification Alert**: Calls `POST notification-service:8085/notify/email` with customer notification.
8. **Returns Combined Response**: Returns order, payment status, transaction ID, and notification status.

---

## 🚀 How to Run

### 1. Run using Maven
```bash
mvn spring-boot:run
```
*(Or using local wrapper if `mvn` path is specified)*

### 2. Run Tests
```bash
mvn test
```

### 3. Docker Container
```bash
docker build -t order-service .
docker run -p 8083:8083 order-service
```

---

## 🧪 Example API Request

### Checkout Request
```bash
curl -X POST http://localhost:8083/orders/checkout \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: order-service-api-key-secret" \
  -d '{
    "username": "john_doe",
    "productId": 1,
    "quantity": 2,
    "cardNumber": "4000-1234-5678-9010"
  }'
```

### Get Orders by Username
```bash
curl -X GET http://localhost:8083/orders/user/john_doe \
  -H "X-API-KEY: order-service-api-key-secret"
```
