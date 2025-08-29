# High-Concurrency-CQRS-Ticketing-Platform
A ticket-purchasing backend designed for high contention and throughput. It uses CQRS to separate the write path (seat reservation and event emission) from the read path (queries and analytics). Redis + Lua performs O(1) atomic seat locks, RabbitMQ decouples user requests from persistence, DynamoDB stores the write model, and MySQL (via JPA) stores the read model.

- Currently working on updating the project structure and implementation.

## Project Structure
- Two Docker images:
    - Server image (write + query APIs)
        - Spring Boot bootstrap, REST controllers (ticket creation & ticket queries), service layer (Redis/Lua seat lock), and RabbitMQ producer that publishes ticket events.

    - Consumer image (projectors)
        - RabbitMQ consumers for SQL and NoSQL pipelines, MySQL and DynamoDB DAO implementations, and persistence configuration to store tickets in each data store.

On AWS, components run on separate EC2 instances. The Server runs on one host, RabbitMQ and the Consumer on another. Redis and DynamoDB run on their own hosts due to managed service restrictions.

## Data Model

Venue: venueId, zones[]

Event: eventId, venueId, name, type, date

Zone: zoneId, ticketPrice, rowCount, colCount

TicketCreation (write DTO / DynamoDB):
ticketId, venueId, eventId, zoneId, row, column, status, createdOn

TicketInfo (read model / MySQL):
ticketId, venueId, eventId, zoneId, row, column, createdOn

DynamoDB attributes:
ticketId(S), venueId(S), eventId(S), zoneId(N), row(S), column(S), status(S), createdOn.


## Architecture
**Write path**
- REST API receives the purchase request → Redis Lua atomically checks/locks a seat → on success publishes an event to RabbitMQ.

**Consumers (projectors)**
- Subscribe to events and write idempotently:
    - DynamoDB (write model) using attribute_not_exists(ticketId) for idempotence.
    - MySQL (read model) using JPA to support queries and counts.


## Latest Updates
- JDBC → Spring Data JPA (read side/MySQL)
    - Less boilerplate and consistent transaction/mapping semantics and simpler tests.

- RabbitMQ-based CQRS
    - change from MySQL Direct write to async, sending message through outbox.
    - Writes lock seats and enqueue events; consumers asynchronously project to DynamoDB/MySQL.

- One-click local bring-up with Docker
    - docker-compose starts Redis, RabbitMQ, MySQL, DynamoDB Local, and both service images.

- Unified DynamoDB schema
    - Standard attribute names/types (e.g., zoneId, createdOn) across producer/consumer, removing 500 errors caused by schema drift.

- DynamoDB Local with -sharedDb
    - All regions/accounts share one local DB file to avoid “written but cannot read” issues in multi-service local testing.

## TODO

- Update Structure to a Microservice structure 
    - Purchase service
    - Query Service 
    - Message terminal service
- Update RabbitMQ to Kafka, better concurrency and persistent support
- Add payment verification functionality
    - hold space for consumer while making payment
    - Payment verification 
    - Undo Purchase, undo seat purchase, release seat hold. 


## Run Locally
- `./mvnw clean package`
    - run in ticketing and RabbitConsumer project
- `./localDockerInitiate.sh`
- `docker-compose down`


## REST API

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
- `GET /api/v1/tickets/{ticketId}`
  - Response:
    ```json
    {
    "ticketId": "UUID",
    "venueId": "Venue1",
    "eventId": "Event1",
    "zoneId": 2,
    "row": "10",
    "column": "b",
    "status": "PAID",
    "createdOn": "time stamp"
    }
    ```
    - Error:  404 Not Found if ticket does not exist.

- `GET /api/v1/tickets/count/{eventId}`
  - Returns how many tickets sold per zone.
    ```json
    {
    "eventId": "Event1",
    "zones": [
        { "zoneId": 1, "soldCount": 120 },
        { "zoneId": 2, "soldCount": 300 }
    ]
    }
    ```

- `GET /api/v1/tickets/money/{venueId}/{eventId}"`
    - Response:
        ```JSON
        {
        "eventId": "Event1",
        "ticketPrice": 120.0
        }
        ```