# High-Concurrency-CQRS-Ticketing-Platform

A ticket-purchasing backend designed for high contention and throughput.
It uses CQRS to separate the write path (seat reservation and event emission) from the read path (queries and
analytics).
Redis + Lua performs O(1) atomic seat locks, Kafka serves as the event store and message broker, and MySQL (via Spring Data JPA) stores the read model projection.

## Latest Updates

**Latest Updates**

- [DONE] Migrated read-side queries from JDBC to Spring Data JPA (MySQL)
    - Reduced boilerplate code and improved transaction handling for complex queries
    - Kept high-performance JDBC for speed critical writes
- [DONE] Migrated to Kafka for message streaming
    - Better concurrency support and persistent message handling
    - Use dead letter queue to catch Kafka publish errors, make sure no message loss
- [DONE] Modular deployment system with secure environment management
- [DONE] One-click local deployment with Docker Compose
    - Spin up Redis, Kafka, MySQL, and all microservices in one command
- [DONE] Event-sourced CQRS architecture with Kafka as event store

## TODO

- **Virtual Thread Integration**: Multi-threaded purchase and Kafka consumer processing optimization
- **Search Page**: Event search and filtering functionality  
- **Login System**: User authentication with JWT
- **Shopping Cart**: Temporary seat hold with payment verification
- **Frontend Management**: Ticket selection area and admin interface

### Architecture Diagram

```mermaid
flowchart LR
  Client -->|POST ticket| PurchaseService
  PurchaseService -->|Redis seat lock| Redis
  PurchaseService -->|Publish Event| KafkaCluster
  
  subgraph KafkaCluster[Kafka Cluster - Event Store]
    Kafka1[Kafka-1:9092]
    Kafka2[Kafka-2:9092] 
    Kafka3[Kafka-3:9092]
  end
  
  KafkaCluster --> MqProjectionService
  MqProjectionService -->|Project to Read Model| MySQL
  QueryService -->|Query| MySQL
  Client -->|GET ticket| QueryService
```

## Architecture

- ### Structure
    - **Purchase Service (Write API)**
      - Spring Boot REST controllers (ticket creation)
      - Service layer (Redis + Lua for atomic seat lock)
      - **Event-sourced architecture:**
          - Publishes `TicketCreatedEvent` via Spring Events (in-memory)
          - `TicketEventListener` captures events and publishes to **Kafka** (via Spring Cloud Stream)
          - No direct database persistence - Kafka serves as the event store and source of truth
      
    - **MqProjection Service (Read Model Projector)**
      - Spring Boot service consuming ticket events from **Kafka**
      - Projects events into **MySQL** (read-optimized model)
      - Uses retry + dead-letter handling for reliable, idempotent projection

    - **Query Service (Read API)**
        - Exposes REST APIs for:
            - Fetching a ticket by `ticketId`
            - Counting sold tickets per event/zone
            - Aggregating ticket revenue
        - Uses **Spring Data JPA** to query the MySQL read model

- ### Route
    - **Write Path**
        - REST API receives the purchase request
            - → Redis Lua atomically checks and locks a seat
            - → Publishes Spring Event (`TicketCreatedEvent`) in-memory
            - → `TicketEventListener` captures event and sends to Kafka via Spring Cloud Stream
            - → Kafka stores the event (event sourcing - Kafka is the source of truth)
    - **Read Path (Event Projection)**
        - Kafka consumer (`MqProjectionService`) subscribes to ticket events
            - → Projects ticket data into MySQL (read model)
            - → MySQL read model supports queries, counts, and analytics
            - → `QueryService` exposes REST APIs for querying the read model

## Local Deployment Guide

### Quick Start

***- Step 1: Create Environment File***
- Go to the deployment directory and create a .env file according to the .env.template

***- Step 2: Ensure Docker Is Running***

***- Step 3: Run All Services***

1. Grant execute permissions for the startup and shutdown scripts:
```bash 
- chmod +x composeUp.sh composeDown.sh
```
2. Then start all services with:
```bash
./composeUp.sh
```
   - If you want detailed logs for debugging or monitoring startup sequence: ```./composeUp.sh --verbose```

***- Step 4: Stop All Services***

```bash
./composeDown.sh
```

## Access Points
| Service | URL | Description |
|----------|-----|-------------|
| **Kafka UI** | [http://localhost:8088](http://localhost:8088) | Kafka cluster dashboard |
| **Purchase Service** | [http://localhost:8080](http://localhost:8080) | Write API (seat lock + event publish) |
| **Query Service** | [http://localhost:8081](http://localhost:8081) | Read API (analytics + queries) |


## REST API
### Health Check
- `http://localhost:8080/api/v1/health`
  - Healthy Response: ```Purchase Service is healthy!```
- `http://localhost:8081/api/v1/health`
  - Healthy Response: ```Query Service is healthy! Available endpoints: /tickets/{id}, /tickets, /tickets/count/{eventId}```

### Purchase API (Write Path)

- `POST /api/v1/tickets`
    - Request Body:
      ```json
      {
        "venueId": "Venue1",
        "eventId": "Event1",
        "zoneId": 2,
        "row": "10",
        "column": "b"
      }
      ```
    - Response (201 Created):
      ```json
          {
          "ticketId": "UUID",
          "zoneId": 2,
          "row": "b",
          "column": "10",
          "createdOn": "time stamp"
          }
      ```

### Query API (Read Path)
- `GET /api/v1/tickets/tickets`
- `GET /api/v1/tickets/{ticketId}`
- `GET /api/v1/tickets/count/{eventId}`
- `GET /api/v1/tickets/money/{venueId}/{eventId}"`
