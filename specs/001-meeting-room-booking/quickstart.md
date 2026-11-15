# Quickstart Validation

## Prerequisites

1. Java 17 and Maven 3.9+.
2. MySQL 8 running at `localhost:3306` with user `root` and password `1234`.
3. Create database `room_booking` or allow the documented setup command to create it.

## Run

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/room_booking?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='1234'
mvn spring-boot:run
```

The migration seeds five rooms and development users. Use `X-User-Id` with the seeded external IDs listed in `docs/backend-guide.md`.

## Core checks

1. `GET /api/v1/rooms` as `student-demo` returns five rooms.
2. `GET /api/v1/rooms/7081/schedule?date=2026-12-01` returns public occupancy only.
3. `POST /api/v1/reservations` with `X-User-Id: student-demo` creates `PENDING`; the same room/time with `teacher-demo` returns a conflict.
4. `GET /api/v1/admin/reservations?status=PENDING` and `POST /api/v1/admin/reservations/{id}/approve` as `manager-demo` confirms the student request.
5. `POST /api/v1/reservations/{id}/cancel` before start releases the interval and creates a notification.
6. `POST /api/v1/admin/closures/preview` then `POST /api/v1/admin/closures` cancels affected records and writes logs.

Contract details and request/response examples are in `contracts/openapi.yaml` and `docs/api.md`.
