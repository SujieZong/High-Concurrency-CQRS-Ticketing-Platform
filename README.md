# High-Concurrency-CQRS-Ticketing-Platform
A high-contention ticketing backend using CQRS. The write model is persisted synchronously to DynamoDB (source of truth). Read queries are served from a MySQL materialized view that is asynchronously projected via RabbitMQ events. Redis + Lua provides O(1) atomic seat locking. An Outbox pattern guarantees event delivery to RabbitMQ.

##Project Structure
 
Created three Spring Boot services (each has its own Docker image)—currently 2 services:

- reservation-service (write API)
  - REST controllers for ticket purchase.
  - Redis/Lua seat lock (hold/release).
  - Synchronous write to DynamoDB (write model / SoT).
  - Outbox table + relay: serialize ticket.created → publish to RabbitMQ.
- projector-service (read projector / consumer)
  - Subscribes to ticket.created from RabbitMQ.
  - Writes idempotently to MySQL (read model) only.
  - No DynamoDB writes here (NoSQL consumer removed).
- query-service (read API) -- Working on separating
  - REST controllers for ticket queries & counts.
  - Connects read-only to MySQL (no writes).

Local dev uses docker-compose to bring up: Redis, RabbitMQ, MySQL, DynamoDB Local, plus the 3 services.
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


## Latest Finished Updates
- JDBC → Spring Data JPA (read side/MySQL)
    - Less boilerplate and consistent transaction/mapping semantics and simpler tests.

- Direct Dynamo Write on the write path
  - reservation-service writes to DynamoDB synchronously.
  - Outbox → RabbitMQ → projector-service → MySQL.
  - Removed DynamoDB writes from projector-service (NoSQL consumer & its queue/bindings deleted).

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