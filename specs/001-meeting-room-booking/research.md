# Research: 会议室预约后端

## Decision 1: Java 17 + Spring Boot 3

- **Decision**: 使用 Java 17 LTS、Spring Boot 3.3、Spring MVC、Bean Validation 和 Spring Data JPA。
- **Rationale**: 与用户指定语言一致，适合构建 REST API，能够通过声明式事务和成熟的校验机制承载预约状态和权限规则。
- **Alternatives considered**: Spring Boot 2 需要旧版 Java；纯 Servlet/JDBC 会增加样板代码和事务边界维护成本。

## Decision 2: MySQL 8 + Flyway

- **Decision**: 使用用户提供的 `localhost:3306`, `root`, `1234` 连接默认数据库 `room_booking`，通过环境变量覆盖，使用 Flyway 初始化表结构和种子房间。
- **Rationale**: 便于课程项目本地运行、审查和重建；迁移脚本可追踪数据库变更。
- **Alternatives considered**: Hibernate 自动建表无法稳定表达初始化数据和版本演进；内存数据库不满足持久化验收。

## Decision 3: Server-authoritative identity adapter

- **Decision**: 生产使用微信小程序 `wx.login` code 换取 `openid`，后端签发随机 Bearer 会话令牌并只保存令牌哈希；本地保留 `X-User-Id` 请求头适配器和开发种子账号，不保存学校密码。
- **Rationale**: 微信会话是小程序登录入口，业务资格仍需负责人审核；服务端会话令牌避免把 `openid` 直接当作业务授权凭证。
- **Alternatives considered**: 用户名密码会引入需求未要求的密码生命周期和存储风险；让客户端提交角色或直接使用 `openid` 作为授权凭证不满足服务端权限要求。

## Decision 4: Transactional overlap and idempotency

- **Decision**: 预约写入、申请确认、本人取消和停用处理都在事务中完成；按房间和时间查询有效记录并使用悲观写锁；请求标识对用户唯一。
- **Rationale**: 待确认记录同样占用，多个负责人和用户可能同时操作，必须将检查和写入置于同一事务。
- **Alternatives considered**: 仅在控制器中先查后写会产生竞态；异步队列会让课程项目的即时结果和错误反馈变复杂。

## Decision 5: REST contract and privacy projection

- **Decision**: `/api/v1` JSON REST API；公共日程只返回占用状态，预约详情按本人/负责人投影字段；统一错误结构包含 code、message、details。
- **Rationale**: 与微信小程序消费方式匹配，避免把隐私字段混入公共日程。
- **Alternatives considered**: GraphQL 对本期固定接口没有必要；直接暴露实体会导致权限投影遗漏。
