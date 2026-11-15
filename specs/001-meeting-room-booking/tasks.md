# Tasks: 会议室预约后端

**Input**: Design documents from `/specs/001-meeting-room-booking/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Create Maven Spring Boot project descriptor in `pom.xml` with Java 17, Web, JPA, Validation, MySQL, Flyway and Lombok dependencies
- [X] T002 [P] Create application configuration and environment overrides in `src/main/resources/application.yml`
- [X] T003 [P] Create Java package entry point in `src/main/java/cn/ccnu/roombooking/RoomBookingApplication.java`
- [X] T004 [P] Create Java ignore rules in `.gitignore`

## Phase 2: Foundational (Blocking Prerequisites)

- [X] T005 Create MySQL schema, indexes, seed rooms and development users in `src/main/resources/db/migration/V1__init.sql`
- [X] T006 [P] Create domain enums for identity, member, reservation, closure and notification states in `src/main/java/cn/ccnu/roombooking/domain/Enums.java`
- [X] T007 [P] Create JPA entities for users, reviews, rooms, reservations, closures, notifications and operation logs in `src/main/java/cn/ccnu/roombooking/domain/*.java`
- [X] T008 [P] Create repository interfaces and locked overlap queries in `src/main/java/cn/ccnu/roombooking/domain/*Repository.java`
- [X] T009 [P] Create API envelope, error types, exception handler, clock and request identity context in `src/main/java/cn/ccnu/roombooking/common/`
- [X] T010 [P] Create header-based development authentication and manager authorization in `src/main/java/cn/ccnu/roombooking/config/AuthConfig.java`
- [X] T011 Create shared DTO validation and response projection helpers in `src/main/java/cn/ccnu/roombooking/common/ApiDtos.java`

## Phase 3: User Story 1 - 身份提交与审核 (Priority: P1)

**Goal**: 用户提交身份资料，负责人审核并授予学生或教师权限。

**Independent Test**: Submit identity, approve/reject it, resubmit after rejection, and verify unverified users cannot access business endpoints.

- [X] T012 [US1] Implement transactional identity submission, duplicate employee number checks, review creation and notification in `src/main/java/cn/ccnu/roombooking/service/IdentityService.java`
- [X] T013 [US1] Implement identity review and re-review endpoints in `src/main/java/cn/ccnu/roombooking/web/IdentityController.java`
- [X] T014 [US1] Add identity status and manager authorization guards to `src/main/java/cn/ccnu/roombooking/config/AuthConfig.java`

## Phase 4: User Story 2 - 房间与日程查询 (Priority: P1)

**Goal**: 已核验师生查看五间房间、公共占用日程和本人详情边界。

**Independent Test**: Query rooms and a date as a student; verify unknown data, closures, non-open periods and privacy projection.

- [X] T015 [US2] Implement room and public schedule query service with on-read expiry/completion handling in `src/main/java/cn/ccnu/roombooking/service/RoomService.java`
- [X] T016 [US2] Implement room list and schedule endpoints in `src/main/java/cn/ccnu/roombooking/web/RoomController.java`
- [X] T017 [US2] Add public schedule projection that omits other users' name, phone, reason and review fields in `src/main/java/cn/ccnu/roombooking/common/ApiDtos.java`

## Phase 5: User Story 3 - 提交和取消预约 (Priority: P1)

**Goal**: 学生创建待确认占用，教师直接成功，服务端原子校验冲突并支持本人开始前取消。

**Independent Test**: Submit valid, conflicting, over-capacity, out-of-hours and repeated requests, then cancel before start.

- [X] T018 [US3] Implement Beijing-time validation, capacity validation, closure/overlap locking, role-derived status and request idempotency in `src/main/java/cn/ccnu/roombooking/service/ReservationService.java`
- [X] T019 [US3] Implement create, list own, detail and cancel reservation endpoints in `src/main/java/cn/ccnu/roombooking/web/ReservationController.java`
- [X] T020 [US3] Add consistent conflict, validation, unauthorized and state-transition errors in `src/main/java/cn/ccnu/roombooking/common/ApiExceptionHandler.java`

## Phase 6: User Story 4 - 负责人处理申请和资源停用 (Priority: P1)

**Goal**: 负责人确认/拒绝申请，维护房间资料，预览并执行停用影响。

**Independent Test**: Process one request concurrently as two managers and create a closure that cancels all overlapping unfinished records once.

- [X] T021 [US4] Implement approve/reject with conditional state transitions, revalidation, reasons, notifications and operation logs in `src/main/java/cn/ccnu/roombooking/service/AdminService.java`
- [X] T022 [US4] Implement closure preview, create/impact cancellation and release endpoints in `src/main/java/cn/ccnu/roombooking/web/AdminController.java`
- [X] T023 [US4] Implement manager room metadata updates and manager reservation ledger queries in `src/main/java/cn/ccnu/roombooking/service/AdminService.java`

## Phase 7: User Story 5 - 结果通知与管理台账 (Priority: P2)

**Goal**: 用户查看通知，负责人查询台账和日志。

**Independent Test**: Trigger review, approval, rejection, cancellation, expiry and closure events, then verify notification and audit records.

- [X] T024 [US5] Implement notification list/read endpoints and user reservation history projection in `src/main/java/cn/ccnu/roombooking/web/NotificationController.java`
- [X] T025 [US5] Implement operation log persistence and manager filtering endpoints in `src/main/java/cn/ccnu/roombooking/web/AdminController.java`
- [X] T026 [US5] Implement on-read expiry/completion reconciliation used by all relevant queries in `src/main/java/cn/ccnu/roombooking/service/ReservationLifecycleService.java`

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T027 [P] Write backend architecture, configuration, seed accounts and deployment notes in `docs/backend-guide.md`
- [X] T028 [P] Write endpoint request/response examples, error codes and privacy rules in `docs/api.md`
- [X] T029 [P] Copy and align the OpenAPI contract with implemented endpoints in `docs/openapi.yaml`
- [X] T030 Run the quickstart validation and fix compilation/runtime issues documented by `specs/001-meeting-room-booking/quickstart.md`

## Dependencies & Execution Order

- Setup (T001-T004) precedes Foundational (T005-T011).
- Foundational precedes every user story.
- US1, US2, US3 and US4 are P1; implement sequentially because they share domain and service files. US5 follows lifecycle and notification support.
- Documentation tasks T027-T029 can run in parallel after endpoints stabilize. T030 is final validation.

## Implementation Strategy

1. Complete setup and foundation.
2. Deliver the MVP slice: identity, room query and reservation create/cancel (US1-US3).
3. Add manager processing and closure (US4).
4. Add notification, lifecycle and ledger projections (US5).
5. Finish documentation and quickstart verification.
