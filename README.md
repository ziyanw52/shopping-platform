## SYSTEM ARCHITECTURE OVERVIEW

### Five Independent Microservices:

```
┌─────────────────────────────────────────────────────────┐
│                   Shopping Platform                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │   Auth       │  │   Account    │  │    Item      │ │
│  │   Service    │  │   Service    │  │   Service    │ │
│  │ (MySQL)      │  │ (MySQL)      │  │ (MongoDB)    │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
│                                                         │
│  ┌──────────────┐          ┌──────────────┐           │
│  │   Order      │  ◄────►  │   Payment    │           │
│  │   Service    │  Sync &  │   Service    │           │
│  │ (Cassandra)  │  Kafka   │ (Cassandra)  │           │
│  └──────────────┘          └──────────────┘           │
│                                                         │
└─────────────────────────────────────────────────────────┘
```
### Services at a Glance

```
Service          Port   Database   Key Purpose
─────────────────────────────────────────────────────
Auth Service     8085   MySQL      User login, JWT generation
Account Service  8081   MySQL      User profiles, addresses
Item Service     8082   MongoDB    Product catalog, inventory
Order Service    8083   MySQL      Order creation, status tracking
Payment Service  8084   Cassandra  Payment processing, orchestration

```

### Communication Pattern

```
HTTP/Feign (Synchronous):
    Payment Service ──HTTP──> Order Service
    Payment Service ──HTTP──> Item Service

Kafka (Asynchronous):
    Payment Service ──Event──> Kafka Topics
                              payment.success
                              payment.failed


```
### HOW 5 SERVICES COMMUNICATE

1. INDEPENDENT SERVICES (No communication needed)- Only provide API,no service-to-service communication
   - Auth Service
   - Account Service  
   - Item Service

2. ORCHESTRATED SERVICES (Payment Service coordinates)
   - Payment Service ←→ Order Service (HTTP)
   - Payment Service ←→ Item Service (HTTP)
   - Payment Service → Kafka (Events)

3. MESSAGE BROKER (Async communication)
   - Kafka (optional listeners)


#### Service 1: AUTH SERVICE (8085)
INPUT: User registration/login
OUTPUT: JWT Token
COMMUNICATION: None (standalone)

Flow:
User → Auth Service (REST API)
         ↓
    Database (MySQL) - store/validate credentials
         ↓
    Return JWT Token
No communication with other services. It's independent.

#### ACCOUNT SERVICE (8081)
INPUT: User profile, addresses, payment methods
OUTPUT: Account details
COMMUNICATION: None (standalone)

Flow:
User → Account Service (REST API)
         ↓
    Database (MySQL) - store profile
         ↓
    Return account info

#### Service 3: ITEM SERVICE (8082)
INPUT: Product queries, inventory operations
OUTPUT: Product info, stock levels
COMMUNICATION: Receives HTTP calls FROM Payment Service

Flow:
Item Service listening on HTTP endpoints:
  - GET /items
  - POST /items/{id}/reserve?quantity=X
  - POST /items/{id}/restock?quantity=X

Receives calls FROM Payment Service when payment succeeds/fails.

#### Service 4: ORDER SERVICE (8083)
INPUT: Order creation, status updates
OUTPUT: Order details
COMMUNICATION: Receives HTTP calls FROM Payment Service

Flow:
Order Service listening on HTTP endpoints:
  - POST /orders/create
  - POST /orders/{id}/confirm
  - POST /orders/{id}/payment-failed
  - GET /orders/{id}
Receives calls FROM Payment Service when payment succeeds/fails.

#### Service 5: PAYMENT SERVICE (8084) - THE ORCHESTRATOR
This is the KEY service that coordinates everything!

INPUT: Payment request
OUTPUT: Payment confirmation or failure

COMMUNICATION:
  • Calls Order Service (HTTP/Feign)
  • Calls Item Service (HTTP/Feign)
  • Publishes to Kafka (Events)

- Kafka -> may have duplicates (at-least-one)
idempotency：Cassandra use unique Request ID （request_id = PRIMARY KEY）

```
Kafka Consumer
        │
        ▼
Receive PaymentEvent
        │
        ▼
Check requestId in Cassandra
        │
        ├─ Exists → ignore
        │
        └─ Not exists
              │
              ▼
        simulate payment
              │
              ▼
        insert Cassandra
              │
              ▼
        publish payment-success
```
---
