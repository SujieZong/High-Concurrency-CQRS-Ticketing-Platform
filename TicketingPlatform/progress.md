📌 高并发票务平台 — 当前进展总结

1. 数据访问层（DAO → JPA）
   •	旧状态：使用手写的 JdbcTicketDao 和 DynamoTicketDao 进行数据库访问。
   •	已完成：
   •	全部迁移至 Spring Data JPA (JpaRepository)。
   •	所有数据库表（event, ticket, venue, zone）都已有对应的 @Entity 模型。
   •	关系映射已补全（@OneToMany, @ManyToOne 等）。
   •	status 字段已改为 enum TicketStatus，通过 @Enumerated(EnumType.STRING) 映射，保证类型安全。

✅ DAO 层彻底现代化，支持更简洁的 CRUD 和聚合查询。

⸻

2. DTO 与 Entity 的职责分离
   •	model 包：只包含数据库实体（带 JPA 注解的持久化对象）。
   •	dto 包：只包含传输对象，例如：
   •	TicketCreationDTO（用于创建指令，Controller → Service）
   •	TicketRespondDTO（Service → Controller → 前端响应）
   •	TicketInfoDTO（查询结果投影，QueryService → Controller → 前端）
   •	MqDTO（未来对接消息队列时使用）

✅ 边界清晰，避免 DTO 和 Entity 混用。

⸻

3. 核心业务逻辑（Command）
   •	TicketService（实现了 TicketServiceInterface）
   •	入口方法：purchaseTicket(TicketCreationDTO)
   •	逻辑流程：
    1.	占座：通过 SeatOccupiedRedisFacade.tryOccupySeat() 调用 Redis + Lua 脚本保证原子性，防止超卖。
    2.	创建实体：DTO → Entity（通过 TicketMapper），在 Service 内部补充业务字段：
          •	ticketId（UUID / 可读格式生成）
          •	createdOn（服务端时间 Instant.now()）
          •	status = PENDING_PAYMENT
    3.	持久化：调用 TicketInfoRepository.save() 写入 MySQL。
    4.	失败补偿：如写库失败，调用 safeReleaseSeat() 释放 Redis 占座。
    5.	返回响应：将保存后的实体映射为 TicketRespondDTO 返回给前端。
          •	异常：
          •	自定义 CreateTicketException，失败时抛出；统一由 GeneralExceptionHandler 处理。

✅ TicketService 已经完成重构，走同步下单逻辑，生成 PENDING_PAYMENT 的待支付票据。

⸻

4. 查询逻辑（Query）
   •	QueryService（实现了 QueryServiceInterface）
   •	getTicket(ticketId) → 查询单票，不存在时抛 TicketNotFoundException。
   •	countTicketSoldByEvent(eventId) → 使用 countByEventId。
   •	sumRevenueByVenueAndEvent(venueId, eventId) → 使用 JPA + JOIN 查询票务收入。
   •	读写分离：Command → TicketService；Query → QueryService。

✅ 已实现 CQRS 的第一步（读写分离）。

⸻

5. 全局异常处理
   •	GeneralExceptionHandler (@RestControllerAdvice) 已完成：
   •	捕获 TicketNotFoundException → 返回 404。
   •	捕获 CreateTicketException → 返回 500（或者 400，视具体情况）。
   •	未来可扩展：统一返回 ErrorMessage JSON（含 code/message/path/traceId）。

✅ 异常处理统一化，避免裸露堆栈。

⸻

6. 控制器层
   •	TicketPurchaseController（旧版逻辑）：仍需重构，尚未接入最新的 TicketService。
   •	需要更新为：
   •	接收 TicketPurchaseRequestDTO (@RequestBody)
   •	在 Controller 内生成 ticketId + createdOn
   •	组装成 TicketCreationDTO
   •	调用 ticketService.purchaseTicket()
   •	返回 TicketRespondDTO 封装在 ResponseEntity
   •	TicketQueryController：已经接入 QueryService，负责查询逻辑。

⸻

✅ 下一步任务

立即要做的：重构 TicketPurchaseController，完成创建票据接口的现代化。
流程如下：
1.	接收 TicketPurchaseRequestDTO（只含用户输入）。
2.	在 Controller 中生成 UUID.randomUUID().toString() 和 Instant.now()。
3.	组装 TicketCreationDTO。
4.	调用 ticketService.purchaseTicket()。
5.	返回 ResponseEntity<TicketRespondDTO>。

⸻

📌 项目蓝图（已对齐）
•	当前：Redis 占座 + JPA 写订单（PENDING_PAYMENT）。
•	下一步：
1.	接入支付接口，支持支付确认 → 更新状态 PAID。
2.	增加超时取消（TTL + 定时任务 → 状态改为 EXPIRED 并释放座位）。
3.	升级消息队列 RabbitMQ → Kafka，实现异步读模型更新。
4.	部署到云端（Docker 容器化 → Kafka 集群 → Redis 主从）。

⸻

👉 这样总结后，下一位接手的人只要看这份进展说明，就能快速理解系统现状和开发边界。

要不要我顺便帮你把 新版 TicketPurchaseController 的完整实现写出来，直接贴进项目即可？