# Implementation Plan: 会议室预约后端

**Branch**: `001-meeting-room-booking` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-meeting-room-booking/spec.md`

**Note**: This template is filled in by the `$speckit-plan` command; its definition describes the execution workflow.

## Summary

实现会议室预约领域的 REST 后端，覆盖身份审核、房间与日程、预约冲突控制、负责人处理、停用、通知和日志。使用 Java 17 + Spring Boot 3 + Spring Data JPA + MySQL 8；在服务层通过事务和数据库约束保证重叠检查、幂等和状态迁移一致性。

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Java 17

**Primary Dependencies**: Spring Boot 3.3, Spring Web, Spring Data JPA, Bean Validation, MySQL Connector/J, Flyway, Lombok

**Storage**: MySQL 8 at localhost:3306, database `room_booking`; username `root`, password `1234` by default, environment variables override

**Testing**: Maven test, Spring Boot context tests, service unit tests, repository integration tests with configurable MySQL

**Target Platform**: Java 17 server on Windows/Linux; REST API consumed by WeChat mini program

**Project Type**: Web service / REST API

**Performance Goals**: 20 concurrent users for 5 minutes; P95 for query/submit <= 2 seconds in the stated acceptance dataset

**Constraints**: Beijing time; same-day 08:00-22:00; no password storage; privacy filtering by role; state transitions authoritative on server

**Scale/Scope**: 5 fixed rooms, 100 accounts, 10,000 historical reservations, P0 backend only; no mini program UI or real SSO

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution file is still the generated placeholder and defines no enforceable project principles. The plan adopts the requirements' explicit privacy, auditability, server-side authorization and consistency constraints. Gate: PASS.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file ($speckit-plan command output)
├── research.md          # Phase 0 output ($speckit-plan command)
├── data-model.md        # Phase 1 output ($speckit-plan command)
├── quickstart.md        # Phase 1 output ($speckit-plan command)
├── contracts/           # Phase 1 output ($speckit-plan command)
└── tasks.md             # Phase 2 output ($speckit-tasks command - NOT created by $speckit-plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
room-backend/
├── pom.xml
├── src/main/java/cn/ccnu/roombooking/
│   ├── RoomBookingApplication.java
│   ├── common/                 # API envelope, errors, auth context, time
│   ├── config/                 # security, Jackson, seed data
│   ├── domain/                 # entities, enums, repositories
│   ├── service/                # transactional business rules
│   └── web/                    # REST controllers and DTOs
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/V1__init.sql
├── src/test/java/cn/ccnu/roombooking/
└── docs/
    ├── backend-guide.md
    └── api.md
```

**Structure Decision**: 单一 Maven Spring Boot 后端，按 domain/service/web 分层；迁移脚本和配置位于 resources，交付文档位于 docs。生产认证使用微信 `wx.login` code 换取服务端 Bearer 会话，本地保留 `X-User-Id` 开发身份适配器；角色判断始终由服务端完成。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | No constitution violation identified |
