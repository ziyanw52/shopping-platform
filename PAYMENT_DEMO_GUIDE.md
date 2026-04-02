# Payment Service Demo Guide

## Overview
This guide demonstrates the payment service's key features including Kafka event publishing, idempotency, validation, and failure handling.

## Demo Features

### 1. 💰 Normal Payment
Standard payment flow with automatic amount calculation from order details.

**Flow:**
1. Select an order from Orders page
2. Click "Pay Now"
3. Amount is auto-calculated (item price × quantity)
4. Submit payment
5. On success: Kafka event published to `payment.success` topic
6. Order status updated to CONFIRMED

---

### 2. 🔄 Idempotency Test (Kafka Retry Protection)

**Purpose:** Demonstrates that retrying the same request (same requestId) won't create duplicate charges.

**How it works:**
1. Click "Idempotency Test" mode
2. Click "Run Idempotency Test"
3. System sends TWO payment requests with the SAME requestId
4. Both requests return the SAME paymentId
5. Only ONE payment is created in database
6. Only ONE Kafka event is published

**Expected Result:**
```json
{
  "testType": "Idempotency Test",
  "testCase": "Retry Same Request (same requestId)",
  "expectedResult": "✅ Same paymentId returned",
  "actualResult": "✅ PASSED",
  "firstPaymentId": "abc-123",
  "secondPaymentId": "abc-123",  // Same as first!
  "note": "Both requests returned identical paymentId - no duplicate charge"
}
```

**Kafka Behavior:**
- First request: Publishes event to `payment.success`
- Second request: Returns existing payment, NO duplicate event published

---

### 3. ⚠️ Validation Error Tests

Tests input validation to ensure data integrity.

#### Test Cases:

**a) Negative Amount (-10.00)**
- Validates: `@DecimalMin(value = "0.01")`
- Expected: 400 Bad Request
- Error: "Amount must be greater than 0"

**b) Zero Amount (0.00)**
- Validates: `@DecimalMin(value = "0.01")`
- Expected: 400 Bad Request
- Error: "Amount must be greater than 0"

**c) Missing Order ID (null)**
- Validates: `@NotNull`
- Expected: 400 Bad Request
- Error: "Order ID cannot be null"

**d) Empty Request ID**
- Validates: `@NotBlank`
- Expected: 400 Bad Request
- Error: "Request ID (idempotency key) cannot be blank"

**Kafka Behavior:**
- Validation errors occur BEFORE payment processing
- NO Kafka events published for validation failures

---

### 4. ❌ Mock Failure Tests

Simulates payment gateway failures to test error handling and rollback.

#### Test Cases:

**a) Card Declined ($666.66)**
- Trigger: Amount = 666.66
- Gateway Response: "MOCK FAILURE: Card declined - insufficient funds or card blocked"
- Payment Status: FAILED
- Stock: Reserved then RESTORED
- Order Status: PAYMENT_FAILED
- Kafka Event: Published to `payment.failed` topic

**b) Insufficient Funds ($999.99)**
- Trigger: Amount = 999.99
- Gateway Response: "MOCK FAILURE: Insufficient funds in account"
- Payment Status: FAILED
- Stock: Reserved then RESTORED
- Order Status: PAYMENT_FAILED
- Kafka Event: Published to `payment.failed` topic

**c) Amount Exceeds Limit ($10,000)**
- Trigger: Amount >= 10,000.00
- Gateway Response: "MOCK FAILURE: Transaction amount exceeds daily limit ($10,000)"
- Payment Status: FAILED
- Stock: Reserved then RESTORED
- Order Status: PAYMENT_FAILED
- Kafka Event: Published to `payment.failed` topic

**Flow for Failed Payments:**
1. Request received
2. Order validated
3. Stock RESERVED
4. Payment gateway called → FAILS
5. Payment record saved with status=FAILED
6. Stock RESTORED (rollback)
7. Order marked as PAYMENT_FAILED
8. Kafka event published to `payment.failed` topic

---

## Kafka Topics

### payment.success
Published when payment succeeds.

**Event Schema:**
```json
{
  "paymentId": "uuid",
  "requestId": "idempotency-key",
  "orderId": 123,
  "itemId": 456,
  "amount": 99.99,
  "currency": "USD",
  "status": "SUCCESS",
  "timestamp": "2026-04-01T15:30:00"
}
```

### payment.failed
Published when payment fails (after stock reservation).

**Event Schema:**
```json
{
  "paymentId": "uuid",
  "requestId": "idempotency-key",
  "orderId": 123,
  "itemId": 456,
  "amount": 666.66,
  "currency": "USD",
  "status": "FAILED",
  "timestamp": "2026-04-01T15:30:00"
}
```

### payment.refunded
Published when payment is refunded.

**Event Schema:**
```json
{
  "paymentId": "uuid",
  "requestId": "original-request-id",
  "orderId": 123,
  "itemId": 456,
  "amount": 99.99,
  "currency": "USD",
  "status": "REFUNDED",
  "timestamp": "2026-04-01T15:30:00"
}
```

---

## Testing Kafka Events

### View Kafka Topics
```bash
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Consume Events from payment.success
```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment.success \
  --from-beginning
```

### Consume Events from payment.failed
```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment.failed \
  --from-beginning
```

### Consume Events from payment.refunded
```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment.refunded \
  --from-beginning
```

---

## Database Verification

### Check Cassandra for Payments
```bash
docker exec -it cassandra cqlsh

# In cqlsh:
USE payment;
SELECT * FROM payments;

# Check idempotency - should see unique request_id
SELECT payment_id, request_id, order_id, amount, status FROM payments;
```

### Verify Secondary Indexes
```bash
# In cqlsh:
SELECT * FROM payments WHERE request_id = 'your-request-id';
SELECT * FROM payments WHERE order_id = 123;
```

---

## Demo Script

### Complete Demo Flow:

1. **Setup:**
   - Ensure all services are running: `docker-compose ps`
   - Check Cassandra is healthy
   - Open Kafka consumer in terminal to watch events

2. **Normal Payment:**
   - Create an order
   - Go to Payment page
   - Select "Normal Payment" mode
   - Submit payment
   - Verify success
   - Check Kafka consumer for `payment.success` event

3. **Idempotency Test:**
   - Select "Idempotency Test" mode
   - Click "Run Idempotency Test"
   - Verify both requests return same paymentId
   - Check Cassandra - only ONE payment record exists
   - Check Kafka - only ONE event published

4. **Validation Errors:**
   - Select "Validation Errors" mode
   - Test each validation case
   - Verify 400 errors returned
   - Confirm NO Kafka events published

5. **Mock Failures:**
   - Select "Mock Failures" mode
   - Test "Card Declined" ($666.66)
   - Verify payment status = FAILED
   - Check stock was restored
   - Verify Kafka event in `payment.failed` topic
   - Repeat for other failure scenarios

6. **Refund:**
   - Go to Orders page
   - Find a CONFIRMED order
   - Click "Refund"
   - Enter payment ID
   - Verify refund success
   - Check Kafka consumer for `payment.refunded` event
   - Verify stock was restored

---

## Key Concepts Demonstrated

### Idempotency
- Uses `requestId` as idempotency key
- Prevents duplicate charges on retry
- Critical for reliable distributed systems
- Kafka events only published once per unique request

### Validation
- Input validation at API layer
- Prevents invalid data from entering system
- Fast-fail approach saves resources

### Failure Handling
- Graceful degradation
- Automatic rollback (stock restoration)
- Clear error messages
- Failed payments tracked in database

### Event-Driven Architecture
- Kafka events for payment lifecycle
- Decoupled services
- Audit trail
- Enables downstream processing

### Stock Management
- Reserve-then-confirm pattern
- Automatic rollback on failure
- Prevents overselling
- Consistent state management

---

## Troubleshooting

### Cassandra Not Running
```bash
docker-compose up -d cassandra
# Wait 60-90 seconds for initialization
docker-compose restart payment-service
```

### Kafka Events Not Appearing
```bash
# Check Kafka is running
docker ps | grep kafka

# Check topics exist
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Check payment service logs
docker logs payment-service
```

### Payment Service Unhealthy
```bash
# Check logs
docker logs payment-service

# Restart service
docker restart payment-service
```

---

## Architecture Notes

### Payment Flow
```
Client Request
    ↓
Validation Layer (@Valid)
    ↓
Check Idempotency (requestId)
    ↓
Fetch Order Details
    ↓
Validate Amount
    ↓
Reserve Stock
    ↓
Process Payment (Gateway)
    ↓
Save Payment Record
    ↓
Publish Kafka Event
    ↓
Update Order Status
    ↓
Return Response
```

### Failure Recovery
```
Payment Fails
    ↓
Mark Payment as FAILED
    ↓
Restore Stock
    ↓
Mark Order as PAYMENT_FAILED
    ↓
Publish payment.failed Event
    ↓
Return Error Response
```

---

## Configuration

### Kafka Producer Settings (Idempotence Enabled)
```properties
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
spring.kafka.producer.properties.enable.idempotence=true
spring.kafka.producer.properties.max.in.flight.requests.per.connection=5
```

### Cassandra Indexes
```cql
CREATE INDEX IF NOT EXISTS idx_request_id ON payments (request_id);
CREATE INDEX IF NOT EXISTS idx_order_id ON payments (order_id);
```

These indexes enable:
- Fast idempotency checks by requestId
- Prevent duplicate payments per order
