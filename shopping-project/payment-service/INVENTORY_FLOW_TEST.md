# Inventory Deduction & Refund Testing Guide

## Complete Flow Test

### **Setup**
1. Start all services: `docker-compose up -d`
2. Create item in Item Service with initial stock: 100
3. Create order referencing that item

---

## **Test Case 1: Payment Success → Inventory Deducted**

### **Before:**
```
Item: laptop
Stock: 100
```

### **Step 1: Create Item (if not exists)**
```bash
curl -X POST http://localhost:8082/items \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "price": 999.99,
    "stock": 100
  }'
# Response: { "id": "item-123", "name": "Laptop", "stock": 100 }
```

### **Step 2: Create Order**
```bash
curl -X POST http://localhost:8083/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "itemId": "item-123",
    "quantity": 1
  }'
# Response: { "orderId": 101, "itemId": "item-123", "quantity": 1, "status": "CREATED" }
```

### **Step 3: Submit Payment (SUCCESS)**
```bash
curl -X POST http://localhost:8084/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 101,
    "amount": 999.99,
    "currency": "USD",
    "requestId": "req-success-001",
    "paymentMethod": "CARD"
  }'
# Response: 202 Accepted
```

### **Expected Results:**
1. ✅ Payment status: SUCCESS
2. ✅ Order status: CONFIRMED (updated by OrderServiceClient)
3. ✅ Inventory: 100 → **99** (deducted by ItemServiceClient.reserveItem)
4. ✅ Email logged: Success notification
5. ✅ Console logs show: "✓ Reserved 1 units of item item-123 for order"

### **Verify Inventory Deducted:**
```bash
curl -X GET http://localhost:8082/items/item-123
# Expected stock: 99
```

---

## **Test Case 2: Payment Success → then REFUND → Inventory Restored**

### **Continue from Test Case 1**

### **Step 4: Get Payment ID**
```bash
curl -X GET http://localhost:8084/payments/101
# Get the paymentId from response (e.g., "pay-abc123")
```

### **Step 5: Submit Refund**
```bash
curl -X POST http://localhost:8084/payments/{PAYMENT_ID}/refund \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Customer request"
  }'
# Response: 202 Accepted
```

### **Expected Results:**
1. ✅ Payment status: REFUNDED
2. ✅ Order status: REFUNDED (updated by OrderServiceClient)
3. ✅ Inventory: 99 → **100** (restored by ItemServiceClient.restockItem)
4. ✅ Email logged: Refund confirmation
5. ✅ Console logs show: "✓ Restocked 1 units of item item-123 for refunded order"

### **Verify Inventory Restored:**
```bash
curl -X GET http://localhost:8082/items/item-123
# Expected stock: 100
```

---

## **Test Case 3: Insufficient Stock Scenario**

### **Setup:**
- Item stock: 5
- Try to order: 10

### **Step 1: Create Item with low stock**
```bash
curl -X POST http://localhost:8082/items \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mouse",
    "price": 29.99,
    "stock": 5
  }'
# Response: { "id": "item-456", "stock": 5 }
```

### **Step 2: Create Order with quantity > stock**
```bash
curl -X POST http://localhost:8083/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 2,
    "itemId": "item-456",
    "quantity": 10
  }'
# Response: { "orderId": 102, "itemId": "item-456", "quantity": 10 }
```

### **Step 3: Submit Payment (SUCCESS)**
```bash
curl -X POST http://localhost:8084/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 102,
    "amount": 299.90,
    "currency": "USD",
    "requestId": "req-insufficient-001",
    "paymentMethod": "CARD"
  }'
```

### **Expected Results:**
1. ✅ Payment status: SUCCESS
2. ✅ Order status: CONFIRMED
3. ⚠️ Inventory deduction FAILS (not enough stock)
4. ✅ Console logs show: "✗ Failed to reserve inventory for order 102: Not enough stock for item: item-456"
5. ✅ Payment and order succeed despite inventory error (graceful degradation)
6. ✅ Error is logged but doesn't fail the transaction (important for production!)

---

## **Expected Console Output**

### **Payment Success with Inventory Deduction:**
```
✓ PAYMENT SUCCESS EVENT: PaymentId=pay-xyz, OrderId=101, Amount=999.99 USD
✓ Order 101 status updated to CONFIRMED
✓ Retrieved order details for order: 101
✓ Item item-123 reserved with quantity 1
✓ Reserved 1 units of item item-123 for order
✓ Successfully processed payment success event for Order: 101
```

### **Refund with Inventory Restoration:**
```
↩️  PAYMENT REFUNDED EVENT: PaymentId=pay-xyz, OrderId=101, Amount=999.99 USD
✓ Order 101 status updated to REFUNDED
✓ Retrieved order details for order: 101
✓ Item item-123 restocked with quantity 1
✓ Restocked 1 units of item item-123 for refunded order
✓ Successfully processed payment refund event for Order: 101
```

### **Insufficient Stock Error (but payment still succeeds):**
```
✓ PAYMENT SUCCESS EVENT: PaymentId=pay-abc, OrderId=102, Amount=299.90 USD
✓ Order 102 status updated to CONFIRMED
✓ Retrieved order details for order: 102
✗ Item item-456 reserve with quantity 10 failed: Not enough stock
✗ Failed to reserve inventory for order 102: Not enough stock for item: item-456
⚠️ Failed to reserve inventory for order 102: Not enough stock for item: item-456
✓ Successfully processed payment success event for Order: 102
```

---

## **Summary - Full Inventory Cycle**

```
Stock: 100
  ↓
[Payment SUCCESS]
  ↓
Stock: 99 (deducted)
  ↓
[Refund REQUEST]
  ↓
Stock: 100 (restored)
```

---

## **What to Check**

- [ ] Stock value in Item Service changes (GET /items/{id})
- [ ] Console logs show reserve/restock messages
- [ ] Order status updates correctly
- [ ] Payment status updates correctly
- [ ] Insufficient stock error is handled gracefully
- [ ] Emails are logged for all scenarios

Run these tests and let me know the results!
