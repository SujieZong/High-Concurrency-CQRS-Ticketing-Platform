# High-Concurrency-CQRS-Ticketing-Platform
The project implements a streamlined ticket-purchasing application using Spring Boot, Redis, RabbitMQ, DynamoDB, and MySQL. Testing will focus on highly concurrent purchase requests, pushing the system to its limits with intentional seat-conflict scenarios to verify correctness under load.

### Project Structure
To describe the overall structure, I built two distinct Docker images. The first image includes two
REST controllers, one for ticket creation and one for ticket queries. The image includes Spring
Boot bootstrap, the web API layer, the service layer that calls Redis, and the RabbitMQ
producer that publishes ticket events.

The second image contains the RabbitMQ consumers for both SQL and NoSQL pipelines, the
MySQL and DynamoDB DAO implementations, and all persistence configuration needed to
save tickets into each data store.

 On AWS, all parts are running on separate EC2 instances. Server
Instance runs on one server, RabbitMQ and RabbitConsumer run on another server. Redis and
DynamoDB are also running on 2 separate instances since AWS Redis and DynamoDB
services are not allowed to be used.

### Data Model
The data model has the following main models and those are being used in my design to
simulate a real ticket purchasing program backend:
- Venue: venueId, List of Zones
    - The List of zones is different according to different Venues, for future proofing
- Event: eventId, venueId, name, type, date
    - Basic Information of events, combined with VenueId to pin to a specific event.
- Zone: zoneId, ticketPrice, rowCount, colCount
    - The price in my simulation is assigned according to zones.
- TicketCreation: ticketId, venueId, eventId, zoneId, row, column, status, and createdOn
timestamp
    - The write-model DTO carries the object that is generated from the
TicketCreation, which will be written to the NoSQL database
- TicketInfo: ticketId, venueId, eventId, zoneId, row, column, and createdOn timestamp
    - The read model used by query APIs. And also, the structure is saved to the SQL database.

### Summarization
In summary, this ticketing system combines a lightweight API layer with an atomic, in-memory
seat reservation mechanism, an asynchronous message processing, and dual persistence
stores to deliver both high throughput and strong consistency under contention. Redis with Lua
scripts enforces single-seat guarantees at O(1) cost, RabbitMQ decouples writing from the user
path, and DynamoDB/MySQL implement a CQRS-style write vs read model. By careful tuning
the connection pools and consumer threads, the platform scales to thousand of concurrent
users, while JMeter tests confirm that duplicate requests are rejected without throttling overall
throughput. Redis sharding, a read-only cache, and a transparent routing layer will be some of
the future improvements. Those improvements will provide a higher concurrency level, distribute
the access stress of a single server, and provide a better service to the consumers.