# Enable Java 21 Virtual Threads in QueryService (Spring Boot 3.5)

This guide explains, end-to-end, how to enable Java 21 Virtual Threads (Project Loom) for your `QueryService` and validate it safely. It focuses on your current stack: Spring Boot 3.5.x, Spring MVC (servlet), Spring Data JPA, HikariCP, and MySQL.

The steps below do not change any code yet; they show exactly what to add/change so you can follow along and learn the rationale, caveats, and validation approach.

---

## Why Virtual Threads Here

- QueryService is read-only and uses blocking I/O (JDBC). Virtual threads are designed to make blocking I/O scalable by dramatically reducing the cost of each blocked thread.
- Virtual threads do not increase database capacity. They reduce the cost of waiting in the app, but actual DB concurrency is still bounded by your Hikari pool size and the database’s own limits. Combine this with correct pool sizing, read replicas, and caching for best effect.

---

## Prerequisites

- JDK 21+ for build and runtime.
- Spring Boot 3.3+ (your parent is 3.5.5, so you’re good). This gives you simple property switches to enable virtual threads.
- JDBC driver compatible with Java 21 (MySQL Connector/J in your project is fine).

---

## How Enabling Works in Spring Boot 3.5

There are two concerns:

1) Application-wide virtual thread executors (for `@Async`, initialization tasks, blocking work off the request thread).
2) HTTP request handling on virtual threads (so each servlet request is processed on a virtual thread instead of a platform thread).

Spring Boot 3.3+ provides simple properties for both:

```yaml
spring:
  threads:
    virtual:
      enabled: true     # Provide a VirtualThreadPerTaskExecutor for application tasks

server:
  virtual-threads:
    enabled: true       # Run servlet request handling on virtual threads
```

With these enabled, your controllers and service methods (e.g., JPA queries) execute on virtual threads. Blocking JDBC calls still occupy a DB connection, but they no longer tie up a scarce platform thread.

---

## Step-by-Step Changes

1) Enable virtual threads in `QueryService` configuration

   File: `QueryService/src/main/resources/application.yml`

   Add the following properties (keep your existing settings):

   ```yaml
   spring:
     threads:
       virtual:
         enabled: true

   server:
     virtual-threads:
       enabled: true
   ```

   Notes:
   - This does not remove Tomcat’s acceptor/poller threads; it makes the request-processing work execute on virtual threads.
   - No code changes are required to your controllers/services.

2) Revisit Hikari pool sizing (critical)

   You already configure Hikari in `application.yml` via env variables:

   ```yaml
   spring:
     datasource:
       hikari:
         minimum-idle: ${SPRING_DS_MIN_IDLE:5}
         maximum-pool-size: ${SPRING_DS_MAX_POOL:20}
         connection-timeout: ${SPRING_DS_CONN_TIMEOUT:30000}
   ```

   Recommendations when enabling virtual threads:
   - Keep `maximum-pool-size` aligned with actual DB capacity. Virtual threads allow more concurrent waiters, but only `maximum-pool-size` queries can execute on the DB at once.
   - Consider tightening `connection-timeout` to fail fast (e.g., `1000-3000ms`) if the pool is exhausted.
   - Add `max-lifetime` and `keepalive-time` to avoid stale connections and align with DB `wait_timeout`:

     ```yaml
     spring:
       datasource:
         hikari:
           max-lifetime: 1500000     # 25m; set lower than DB wait_timeout
           keepalive-time: 300000    # 5m
     ```

   - Ensure the sum of pool sizes across all app replicas stays well below the DB’s `max_connections` (reserve 10–20% for operations/monitoring).

3) Optional: Manual container customization (only if you cannot use the property)

   For Spring Boot < 3.3, you can set Tomcat’s executor to virtual threads with a bean. You don’t need this on Boot 3.5, but for reference:

   ```java
   // @Configuration in QueryService
   @Bean
   TomcatProtocolHandlerCustomizer<?> virtualThreadExecutor() {
       return protocol -> protocol.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
   }
   ```

---

## Validation Checklist

1) Confirm virtual threads are active
   - Temporarily log the current thread in a controller or service and look for `VirtualThread` in the name/toString(). Example:

     ```java
     log.info("Handling on thread={}", Thread.currentThread());
     ```

   - Start the app and hit an endpoint; you should see something like `VirtualThread[#123]` in logs.

2) Check Actuator metrics (you have `spring-boot-starter-actuator`)
   - Expose metrics if needed:

     ```yaml
     management:
       endpoints:
         web:
           exposure:
             include: health,info,metrics
     ```

   - Observe `jvm.threads.*` metrics. With virtual threads, you should see many fewer platform threads even under high load.

3) Load test with and without virtual threads
   - Keep the same Hikari pool size.
   - Compare p95 latency and error rates as concurrency rises. Expect better stability when many requests are waiting on I/O, but total DB QPS still bounded by the pool and DB capacity.

---

## Operational Considerations

- Concurrency limiting: Virtual threads make it easy to create very large numbers of concurrent operations. Protect the database with:
  - Right-sized Hikari `maximum-pool-size`.
  - Reasonable `connection-timeout` to fail fast when saturated.
  - Optionally, a simple concurrency gate (e.g., semaphore) around the most expensive queries if you observe bursty timeouts.

- Transactions: Your `@Transactional(readOnly = true)` methods work as-is. Transactions are thread-bound; virtual threads behave like platform threads regarding thread-locals.

- Logging MDC: MDC works, but ensure any custom propagation you rely on is not lost across executors. With virtual threads processing requests, standard MVC logging patterns continue to work.

- Blocking code: Avoid long `synchronized` blocks or native calls that can pin carrier threads. Standard JPA/JDBC usage is fine.

---

## When To Add More Than Virtual Threads

Virtual threads solve application-side thread scalability, not database capacity. For higher throughput:

- Add/size read replicas and point QueryService to a replica.
- Optimize queries and indexes; consider covering indexes or partitioning where appropriate.
- Cache hot query results in Redis with short TTL or event-driven invalidation.
- If all of the above are insufficient and data volume is large, evaluate sharding.

---

## Rollback Plan

To disable, simply revert the properties:

```yaml
spring:
  threads:
    virtual:
      enabled: false

server:
  virtual-threads:
    enabled: false
```

No code changes needed.

---

## Quick Reference (What To Change)

1) Add to `QueryService/src/main/resources/application.yml`:

```yaml
spring:
  threads:
    virtual:
      enabled: true
server:
  virtual-threads:
    enabled: true
```

2) Tune Hikari pool and timeouts according to DB capacity.

3) Validate via logs/Actuator and a short load test.

You can now proceed to flip these properties and test without touching the application code.

