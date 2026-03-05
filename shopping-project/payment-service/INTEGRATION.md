# Payment Service Integration Guide

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Payment Service                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  POST /payments → Save to Cassandra → Publish Kafka Event  │
│                                                              │
│  PaymentEventListener (Kafka Consumer)                      │
│  ├─ payment.success   → Send Email → Confirm Order         │
│  ├─ payment.failed    → Send Email → Mark Failed + Retry   │
│  └─ payment.refunded  → Send Email → Refund Order          │
│                                                              │
└─────────────────────────────────────────────────────────────┘
         ↓                                    ↓
    Order Service                      Item Service
    (MySQL)                            (MongoDB)
    ├─ CONFIRMED                       ├─ reserve()
    ├─ PAYMENT_FAILED                  └─ restock()
    └─ REFUNDED
```

## Payment Flow

### 1. **Create Payment (Synchronous)**
```bash
POST /payments
{
  "orderId": 123,
  "amount": 99.99,
  "currency": "USD",
  "requestId": "req-001",
  "paymentMethod": "CARD"
}

Response: 202 Accepted
{
  "paymentId": "pay-xyz",
  "status": "PROCESSING"
}
```

**What happens:**
- Payment saved to Cassandra with composite key `(payment_id, request_id)` for idempotency
- Event published to Kafka topic `payment.success` or `payment.failed`
- User gets response **immediately** (202)

---

### 2. **Async Event Processing (Background)**

**On Success:**
```
Kafka Event Received (payment.success)
  ↓
PaymentEventListener.onPaymentSuccess()
  ├─ Send success email (MockEmailService)
  ├─ Call OrderService: POST /orders/123/confirm
  │   └─ Order status: CONFIRMED
  └─ Log completion
```

**On Failure (e.g., amount = 4242.42):**
```
Kafka Event Received (payment.failed)
  ↓
PaymentEventListener.onPaymentFailed()
  ├─ Send failure email with retry link
  ├─ Call OrderService: POST /orders/123/payment-failed
  │   └─ Order status: PAYMENT_FAILED
  ├─ Schedule async retry (5 min → 10 min → 20 min)
  └─ Log completion
```

**On Refund:**
```
Kafka Event Received (payment.refunded)
  ↓
PaymentEventListener.onPaymentRefunded()
  ├─ Send refund email
  ├─ Call OrderService: POST /orders/123/refund
  │   └─ Order status: REFUNDED
  ├─ Call ItemService: POST /items/{id}/restock?quantity=X
  │   └─ Add items back to inventory
  └─ Log completion
```

---

## Services & Endpoints

### **Payment Service (Port 8084)**
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/payments` | POST | Create payment |
| `/payments/{id}` | GET | Get payment details |
| `/payments/{id}` | PUT | Update payment |
| `/payments/{id}/refund` | POST | Refund payment |

### **Order Service (Port 8082)** - NEW Endpoints
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/orders/{id}/confirm` | POST | Mark order CONFIRMED (payment success) |
| `/orders/{id}/payment-failed` | POST | Mark order PAYMENT_FAILED |
| `/orders/{id}/refund` | POST | Mark order REFUNDED |

### **Item Service (Port 8083)** - NEW Endpoints
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/items/{id}/reserve?quantity=5` | POST | Reduce inventory |
| `/items/{id}/restock?quantity=5` | POST | Add inventory back |

---

## Testing the Integration

### 1. **Start all services:**
```bash
cd /Users/ziyanw/shopping-platform
docker-compose up -d
cd shopping-project
mvn clean install -DskipTests
```

### 2. **Create a payment (success):**
```bash
curl -X POST http://localhost:8084/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 101,
    "amount": 50.00,
    "currency": "USD",
    "requestId": "req-001",
    "paymentMethod": "CARD"
  }'
```

**Expected flow:**
- Payment saved ✓
- Kafka event published ✓
- Listener receives event ✓
- ✓ SUCCESS EMAIL logged to console
- OrderService endpoint called ✓

### 3. **Create a payment (failure):**
```bash
curl -X POST http://localhost:8084/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 102,
    "amount": 4242.42,
    "currency": "USD",
    "requestId": "req-fail-001",
    "paymentMethod": "CARD"
  }'
```

**Expected flow:**
- Payment saved with status FAILED ✓
- Kafka event published ✓
- ✗ FAILURE EMAIL logged
- Retry scheduled (5 min, 10 min, 20 min) ✓

### 4. **Refund a payment:**
```bash
curl -X POST http://localhost:8084/payments/{PAYMENT_ID}/refund \
  -H "Content-Type: application/json" \
  -d '{"reason": "Customer request"}'
```

**Expected flow:**
- Payment status = REFUNDED ✓
- Kafka event published ✓
- ↩️ REFUND EMAIL logged
- Order marked REFUNDED ✓

---

## Retry Logic (Exponential Backoff)

Failed payments retry automatically:
- **Attempt 1**: 5 minutes later
- **Attempt 2**: 10 minutes later
- **Attempt 3**: 20 minutes later
- **Max**: 3 attempts

After max retries, payment is abandoned (customer must manually retry).

---

## OrderStatus Enum (Updated)

```java
CREATED            // Initial state
PENDING_PAYMENT    // Waiting for payment
PAYMENT_FAILED     // Payment failed, can retry
CONFIRMED          // Payment successful
COMPLETED          // Order shipped
REFUNDED           // Order refunded
CANCELLED          // Order cancelled
```

---

## Key Features

✅ **Idempotency**: Composite Cassandra key prevents duplicate payments  
✅ **Async Processing**: User gets instant 202 response  
✅ **Retry Logic**: Auto-retry failed payments with exponential backoff  
✅ **Email Notifications**: Mock email service logs to console  
✅ **Service Integration**: Seamless order/inventory updates  
✅ **Type Safety**: BigDecimal for precise payment amounts  
✅ **Error Handling**: GlobalExceptionHandler returns proper HTTP codes  

---

## TODO (Future Enhancements)

- [ ] Real SMTP email service (replace MockEmailService)
- [ ] Replace in-memory retry cache with database
- [ ] Add payment analytics & reporting
- [ ] Implement real Stripe integration
- [ ] Add payment webhooks for 3rd party gateways
- [ ] Circuit breaker for service failures
