# 🛡️ BookKaro API Gateway (Backend)

The **API Gateway** acts as the distributed entry point for the BookKaro platform. It is built using **Spring Cloud Gateway** and designed for **high availability**, **fault isolation**, and **traffic protection** in a microservices environment.

This layer is responsible for:
- Controlling inbound traffic
- Protecting downstream services from overload
- Enforcing fair usage policies
- Providing resilience at the system boundary

---

## 🏗️ Architectural Overview

The gateway incorporates multiple **cloud-native resilience patterns** to ensure stability under real-world traffic conditions.

---

## 🛠️ Key Architectural Patterns

### 1️⃣ Distributed Rate Limiting (Token Bucket Algorithm)

To prevent abuse and traffic spikes, the gateway enforces **distributed rate limiting** using a token bucket strategy.

**Design Highlights**
- **Centralized State**: Redis is used as the shared store for token buckets across all gateway instances.
- **Atomic Operations**: Token consumption and refill logic is executed via **Lua scripts**, ensuring atomicity and eliminating race conditions.
- **Dynamic Configuration**: Rate limits are resolved at runtime based on the **Route ID**, enabling per-service throttling.

**Why Redis + Lua?**
- Guarantees consistency under concurrent access
- Avoids distributed locking
- Maintains high throughput with minimal latency

---

### 2️⃣ Bulkhead Isolation (Resource Segregation)

To protect the gateway from cascading failures caused by slow or overloaded downstream services, the **Bulkhead Pattern** is applied.

**Implementation Details**
- **Dedicated Thread Pools**: Each route (e.g., Booking, Payment) is assigned its own isolated `Scheduler`.
- **Noisy Neighbor Protection**: A degraded service cannot exhaust resources used by other services.
- **Reactive & Non-Blocking**: Built on **Project Reactor** for high concurrency and throughput.

---

## 🚦 Getting Started

### ✅ Prerequisites

- **Java 17+**
- **Redis**

```bash
  brew install redis
```

[//]: # (---)

### 1️⃣ Infrastructure Setup
Ensure Redis is running before starting the gateway

```bash
  # Start Redis as a background service
  brew services start redis

  # Monitor Redis commands (useful for debugging rate limits)
  redis-cli monitor
```

---

### 2️⃣ Launching the Gateway
From the backend directory:

```bash
  ./mvnw spring-boot:run
```
The gateway will be available at:
```text
http://localhost:8080
```

---
## ⚙️ Configuration & Tuning

All resilience parameters are defined in:
```text
src/main/resources/application.properties
```

### 🔧 Rate Limiting Configuration

The API Gateway enforces **distributed rate limiting** per route using the **Token Bucket algorithm**, with state stored centrally in Redis. This ensures consistent throttling across all gateway instances.

#### Configuration Properties

| Property | Description |
|---------|-------------|
| `ratelimit.[routeId].replenishRate` | Number of tokens added to the bucket per second (steady request rate) |
| `ratelimit.[routeId].burstCapacity` | Maximum number of tokens the bucket can hold (burst tolerance) |

#### Example Configuration

```properties
ratelimit.bookingService.replenishRate=5
ratelimit.bookingService.burstCapacity=10
```
#### Behavior Explained

- Allows **5 requests per second** under normal traffic conditions
- Permits **up to 10 requests instantly** to absorb short-lived traffic spikes
- Requests exceeding the configured burst capacity are **rejected immediately** with **HTTP 429 (Too Many Requests)**


#### Key Characteristics

- **Distributed**: Rate limits are enforced consistently across all API Gateway instances using Redis as a shared state store
- **Atomic**: Token refill and consumption logic executes atomically via **Redis Lua scripts**, preventing race conditions
- **Low Latency**: Eliminates distributed locks and minimizes network round-trips
- **Route-Aware**: Throttling policies are applied per service using the **Route ID**, enabling fine-grained control  

---

### Failure Response

When a client exceeds the configured rate limit, the gateway responds with:

- **HTTP Status**: 429 Too Many Requests
- **Retry Guidance**: Clients are advised to retry after token replenishment

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Slow down! Quota exceeded for bookingService",
  "path": "/booking/get",
  "retryAfterSeconds": 1
}
```