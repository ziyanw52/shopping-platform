# Payment Service Mock Gateway Guide

## Architecture

The Payment Service uses a **Strategy Pattern** with `PaymentGateway` interface:
- `MockPaymentGateway` - For development (no real charges)
- `StripePaymentGateway` - For production (real Stripe API)

## How to Use

### Development Mode (Default)
By default, the service uses **MockPaymentGateway**. No real card charges!

```bash
cd /Users/ziyanw/shopping-platform/shopping-project
mvn clean install -DskipTests
pkill -9 java

# Start payment service
cd payment-service
mvn spring-boot:run
```

The service will use `payment.gateway.type=mock` from `application.properties`.

### Production Mode (Stripe)
To use real Stripe API:

Edit `application.properties`:
```properties
payment.gateway.type=stripe
```

Or set environment variable:
```bash
export PAYMENT_GATEWAY_TYPE=stripe
mvn spring-boot:run
```

---

## Mock Payment Scenarios

### Successful Payment
```bash
curl -X POST http://localhost:8084/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "amount": 100.00,
    "currency": "USD",
    "requestId": "req-12345"
  }'
```

Response: `status: SUCCESS`

### Failed Payment (Mock Decline)
Amount `4242.42` always fails in mock:
```bash
curl -X POST http://localhost:8084/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "amount": 4242.42,
    "currency": "USD",
    "requestId": "req-12346"
  }'
```

Response: `status: FAILED` with reason "Card declined"

### Amount Limit Error (Mock)
Amount > $10,000 fails:
```bash
curl -X POST http://localhost:8084/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "amount": 15000.00,
    "currency": "USD",
    "requestId": "req-12347"
  }'
```

Response: `status: FAILED` with reason "Amount exceeds limit"

---

## Idempotency Testing

### First Request
```bash
curl -X POST http://localhost:8084/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "amount": 50.00,
    "currency": "USD",
    "requestId": "test-idem-001"
  }'
```

Returns `paymentId: "550e8400..."` with `status: SUCCESS`

### Retry Same Request
```bash
curl -X POST http://localhost:8084/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "amount": 50.00,
    "currency": "USD",
    "requestId": "test-idem-001"  ← SAME requestId
  }'
```

Returns **SAME** `paymentId: "550e8400..."` (no duplicate charge!)

---

## 4 Payment APIs

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/payments` | POST | Submit payment (idempotent) |
| `/payments/{paymentId}` | GET | Lookup payment status |
| `/payments/{paymentId}` | PUT | Update payment (amount, currency) |
| `/payments/{paymentId}/refund` | POST | Refund a payment |

---

## Files Created

### Gateway (Strategy Pattern)
- `PaymentGateway.java` - Interface
- `MockPaymentGateway.java` - Mock implementation (dev)
- `StripePaymentGateway.java` - Real implementation (prod)

### Exception
- `PaymentException.java` - Payment-specific exception

### Core Service
- `PaymentStatus.java` - Enum (PENDING, SUCCESS, FAILED, REFUNDED)
- `Payment.java` - Entity for Cassandra
- `PaymentKey.java` - Composite primary key (idempotency)
- `PaymentRequest.java` - DTO for request
- `PaymentResponse.java` - DTO for response
- `PaymentService.java` - Business logic
- `PaymentController.java` - REST endpoints
- `PaymentRepository.java` - Data access

### Kafka
- `PaymentEvent.java` - Event for Kafka topics

### Configuration
- `CassandraConfig.java` - Enable Cassandra repos
- `CassandraSchemaInit.java` - Auto-create tables
- `application.properties` - Config (mock by default)

---

## Next Steps

1. **Test the APIs** in Postman
2. **Verify Cassandra** stores payments
3. **Check Kafka topics** for events
4. **Add email notification consumer** (to send emails on payment status)
5. **Integrate with Order Service** (call payment API when order created)

All set! Payment Service is ready to use with mock payment processing. 🎉
