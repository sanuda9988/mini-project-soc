# SOC Microservices E-Commerce Platform

> **Service-Oriented Computing Assignment** — A production-grade microservices-based e-commerce system built with Spring Boot, React, and Docker.

---

## 📐 Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                          CLIENT LAYER                            │
│              React SPA (Vite + Vanilla CSS) : 5173               │
└─────────────────────────────┬────────────────────────────────────┘
                              │  HTTP
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                        API GATEWAY : 8080                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌────────────────┐   │
│  │ RateLimiterFilter│  │  JwtAuthFilter  │  │   CorsFilter   │   │
│  │  Token Bucket    │  │  JWT → Auth Svc │  │  CORS Headers  │   │
│  │  per-IP (10 req) │  │  /validate      │  │                │   │
│  └─────────────────┘  └─────────────────┘  └────────────────┘   │
│                    GatewayRoutingController                       │
│              (Reverse Proxy + X-API-KEY injection)               │
└──────┬─────────┬──────────┬──────────┬──────────┬───────────────┘
       │         │          │          │          │
   X-API-KEY injected per route
       │         │          │          │          │
       ▼         ▼          ▼          ▼          ▼
  ┌─────────┐ ┌──────────┐ ┌────────┐ ┌────────┐ ┌──────────────┐
  │  Auth   │ │ Product  │ │ Order  │ │Payment │ │Notification  │
  │:8081    │ │ :8082    │ │ :8083  │ │ :8084  │ │  :8085       │
  │         │ │          │ │        │ │        │ │              │
  │ JWT     │ │ Inventory│ │Checkout│ │Payment │ │Email/SMS     │
  │ Register│ │ CRUD     │ │Workflow│ │Process │ │Alerts        │
  │ Login   │ │ StockMgmt│ │        │ │        │ │              │
  └─────────┘ └──────────┘ └───┬────┘ └────────┘ └──────────────┘
                                │  Orchestrates:
                                │  1. Check Product (product-svc)
                                │  2. Reduce Stock  (product-svc)
                                │  3. Create Order
                                │  4. Process Pay   (payment-svc)
                                │  5. Send Alert    (notify-svc)
```

---

## 🗂️ Project Structure

```
MINI PROJECT SOC/
├── api-gateway/              # Spring Boot API Gateway (port 8080)
│   ├── src/main/java/com/example/gateway/
│   │   ├── GatewayApplication.java
│   │   ├── controller/
│   │   │   └── GatewayRoutingController.java   # Reverse proxy
│   │   └── filter/
│   │       ├── CorsFilter.java                 # CORS @Order(0)
│   │       ├── RateLimiterFilter.java           # Rate Limit @Order(1)
│   │       └── JwtAuthFilter.java              # JWT Auth @Order(2)
│   ├── Dockerfile
│   └── pom.xml
│
├── auth-service/             # JWT Auth Service (port 8081)
│   ├── src/main/java/com/example/auth/
│   │   ├── AuthApplication.java
│   │   ├── controller/AuthController.java      # /auth/register, /login, /validate
│   │   ├── model/UserEntity.java
│   │   ├── repository/UserRepository.java
│   │   └── util/JwtUtil.java                  # JWT creation/validation
│   ├── Dockerfile
│   └── pom.xml
│
├── product-service/          # Product Catalogue (port 8082)
│   ├── src/main/java/com/example/product/
│   │   ├── ProductApplication.java             # Seeds 5 products on startup
│   │   ├── controller/ProductController.java   # CRUD + /reduce-stock
│   │   ├── model/ProductEntity.java
│   │   ├── repository/ProductRepository.java
│   │   └── security/ApiKeyFilter.java         # X-API-KEY validation
│   ├── Dockerfile
│   └── pom.xml
│
├── order-service/            # Order Processing (port 8083)
│   ├── src/main/java/com/example/order/
│   │   ├── OrderApplication.java
│   │   ├── controller/OrderController.java     # /orders/checkout (orchestrator)
│   │   ├── model/OrderEntity.java
│   │   ├── repository/OrderRepository.java
│   │   └── security/ApiKeyFilter.java
│   ├── Dockerfile
│   └── pom.xml
│
├── payment-service/          # Payment Processing (port 8084)
│   ├── src/main/java/com/example/payment/
│   │   ├── PaymentApplication.java
│   │   ├── controller/PaymentController.java   # /payments/process
│   │   ├── model/PaymentEntity.java
│   │   ├── repository/PaymentRepository.java
│   │   └── security/ApiKeyFilter.java
│   ├── Dockerfile
│   └── pom.xml
│
├── notification-service/     # Notification Service (port 8085)
│   ├── src/main/java/com/example/notification/
│   │   ├── NotificationApplication.java
│   │   ├── controller/NotificationController.java  # /notify/email, /sms
│   │   ├── model/NotificationEntity.java
│   │   ├── repository/NotificationRepository.java
│   │   └── security/ApiKeyFilter.java
│   ├── Dockerfile
│   └── pom.xml
│
├── client-app/               # React Frontend (port 5173 / 80 in Docker)
│   ├── src/
│   │   ├── App.jsx            # Main app (Auth, Products, Orders, Checkout)
│   │   ├── api.js             # API client (routes via Gateway)
│   │   ├── index.css          # Design system (CSS variables + components)
│   │   └── main.jsx
│   ├── Dockerfile             # Multi-stage: Node build → Nginx serve
│   ├── nginx.conf             # Nginx with API proxy to gateway
│   ├── vite.config.js
│   └── package.json
│
└── docker-compose.yml         # Full stack orchestration
```

---

## 🔐 Security Architecture

### 1. JWT Authentication (Gateway Layer)
- Clients send `Authorization: Bearer <JWT>` header
- `JwtAuthFilter` in the gateway intercepts all protected routes
- Token is validated by calling `auth-service /auth/validate`
- On success, user info (`X-Username`, `X-Role`) is forwarded downstream

### 2. API Key Authorization (Service Layer)
All downstream services (product, order, payment, notification) require:
```
X-API-KEY: <service-specific-secret>
```
The gateway injects the correct key per-route. Direct access without the key returns `401 Unauthorized`.

### 3. Rate Limiting (Gateway Layer)
Token Bucket algorithm per client IP address:
- **Capacity**: 10 requests
- **Refill Rate**: 2 tokens/second
- Exceeding limit returns `429 Too Many Requests`

### 4. CORS (Gateway Layer)
All cross-origin requests are handled at the gateway. Downstream services also have `@CrossOrigin(origins = "*")` for direct access during development.

---

## 🔑 API Reference

All endpoints are accessed via the API Gateway at `http://localhost:8080`.

### Auth Endpoints (Public)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/auth/register` | Register a new user |
| `POST` | `/auth/login` | Login, returns JWT token |
| `GET`  | `/auth/validate?token=...` | Validate a JWT token |

**Register request body:**
```json
{ "username": "alice", "password": "secret123", "role": "USER" }
```

**Login response:**
```json
{ "token": "eyJhbGc...", "username": "alice", "role": "USER" }
```

### Product Endpoints (JWT Required)

| Method | Path | Description |
|--------|------|-------------|
| `GET`  | `/products` | List all products |
| `GET`  | `/products/{id}` | Get product by ID |
| `POST` | `/products` | Create a product (Admin) |
| `PUT`  | `/products/{id}` | Update a product |
| `POST` | `/products/reduce-stock` | Reduce product stock |

### Order Endpoints (JWT Required)

| Method | Path | Description |
|--------|------|-------------|
| `GET`  | `/orders` | List all orders |
| `GET`  | `/orders/user/{username}` | Get user's orders |
| `POST` | `/orders/checkout` | Place an order (full workflow) |

**Checkout request body:**
```json
{
  "username": "alice",
  "productId": 1,
  "quantity": 2,
  "cardNumber": "4000-1234-5678-9010"
}
```

### Payment Endpoints (JWT Required)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/payments/process` | Process a payment |
| `GET`  | `/payments/history` | Get payment history |
| `GET`  | `/payments/order/{orderId}` | Get payment for order |

> **Mock Payment Logic**: Card numbers starting with `3` (e.g., `3700-...`) will simulate a declined payment. All other card formats succeed.

### Notification Endpoints (JWT Required)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/notify/email` | Send email notification |
| `POST` | `/notify/sms` | Send SMS notification |

---

## 🚀 Running the Application

### Option 1: Docker Compose (Recommended)

**Prerequisites:** Docker Desktop installed and running.

```bash
# 1. Open Docker Desktop and wait for the engine to start
# 2. From the project root:
cd "MINI PROJECT SOC"

# 3. Build and start all services
docker compose up --build

# 4. View logs
docker compose logs -f

# 5. Stop all services
docker compose down
```

**Service URLs after startup:**
| Service | URL |
|---------|-----|
| React Client | http://localhost:5173 |
| API Gateway | http://localhost:8080 |
| Auth Service | http://localhost:8081 |
| Product Service | http://localhost:8082 |
| Order Service | http://localhost:8083 |
| Payment Service | http://localhost:8084 |
| Notification Service | http://localhost:8085 |

### Option 2: Individual Services (Requires Java 17+ and Maven)

```bash
# Terminal 1 — Auth Service
cd auth-service
mvn spring-boot:run

# Terminal 2 — Product Service
cd product-service
mvn spring-boot:run

# Terminal 3 — Order Service
cd order-service
mvn spring-boot:run

# Terminal 4 — Payment Service
cd payment-service
mvn spring-boot:run

# Terminal 5 — Notification Service
cd notification-service
mvn spring-boot:run

# Terminal 6 — API Gateway
cd api-gateway
mvn spring-boot:run

# Terminal 7 — React Client (requires Node.js)
cd client-app
npm install
npm run dev
```

### Option 3: Build Only (No Java Required — Docker Multi-Stage)

```bash
# Build a specific service
docker build -t auth-service ./auth-service
docker build -t product-service ./product-service
# etc.
```

---

## 🧪 Testing the API (Using curl)

```bash
# 1. Register a user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"pass123","role":"USER"}'

# 2. Login and get JWT
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"pass123"}'

# Copy the token from the response, then:
TOKEN="eyJhbGc..."  # paste your JWT here

# 3. Browse products
curl http://localhost:8080/products \
  -H "Authorization: Bearer $TOKEN"

# 4. Place an order (checkout)
curl -X POST http://localhost:8080/orders/checkout \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","productId":1,"quantity":1,"cardNumber":"4000-1234-5678-9010"}'

# 5. View your orders
curl http://localhost:8080/orders/user/testuser \
  -H "Authorization: Bearer $TOKEN"

# 6. Test rate limiting (run 11+ times quickly)
for i in $(seq 1 12); do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/auth/register; done
```

---

## 📊 H2 Database Consoles

Each service has an in-memory H2 database accessible during development:

| Service | H2 Console URL | JDBC URL |
|---------|---------------|----------|
| Auth | http://localhost:8081/h2-console | `jdbc:h2:mem:authdb` |
| Product | http://localhost:8082/h2-console | `jdbc:h2:mem:productdb` |
| Order | http://localhost:8083/h2-console | `jdbc:h2:mem:orderdb` |
| Payment | http://localhost:8084/h2-console | `jdbc:h2:mem:paymentdb` |
| Notification | http://localhost:8085/h2-console | `jdbc:h2:mem:notifydb` |

> Username: `sa`, Password: `password`

---

## 🛠️ Technology Stack

| Component | Technology |
|-----------|-----------|
| Microservices | Spring Boot 3.2.5 |
| API Gateway | Spring Boot + Servlet Filters |
| Auth/JWT | JJWT 0.11.5 (HS256) |
| Database | H2 (in-memory, per service) |
| ORM | Spring Data JPA / Hibernate |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Frontend | React 18 + Vite 5 |
| Styling | Vanilla CSS (Design System) |
| Production Server | Nginx 1.25 |
| Containerization | Docker + Docker Compose |

---

## 📝 SOC Concepts Demonstrated

| Concept | Implementation |
|---------|---------------|
| Service Decomposition | 5 independently deployable microservices |
| API Gateway Pattern | Centralized auth, routing, rate limiting |
| Service Composition | Order Service orchestrates 4 other services |
| Loose Coupling | REST/HTTP communication; no shared databases |
| Service Contract | REST APIs with JSON payloads |
| Security | JWT (stateless auth) + API keys (service-to-service) |
| Rate Limiting | Token Bucket algorithm at gateway |
| Containerization | Docker + Compose for full-stack deployment |
| Service Discovery | Docker DNS (container names as hostnames) |
