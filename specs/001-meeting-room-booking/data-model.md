# Data Model: 会议室预约后端

## User

| Field | Constraint |
|---|---|
| id | UUID primary key |
| externalId | unique, required; development identity key |
| name | 1-50 characters, required |
| employeeNo | 1-30 characters, unique among valid bindings, required |
| memberType | STUDENT or TEACHER |
| phone | mainland China 11-digit format, required |
| identityStatus | UNVERIFIED, PENDING, APPROVED, REJECTED, REVOKED |
| manager | separate boolean authorization, never inferred from memberType |

## IdentityReview

Stores a submitted identity snapshot, reviewer, result, reason, submittedAt and handledAt. A rejected user may create a new review while prior records remain immutable.

## Room

Five seeded records: 7060, 7081, 7091, 7119, 7052. Room number is unique. Capacity is a positive integer or null; device and restriction text may contain the literal `待核实`.

## Reservation

Stores applicant and contact snapshot, room, local date, start/end minute, reason (1-200), attendeeCount (positive), optional note (<=500), status, requestId, reviewer and cancellation/rejection reason. Statuses: PENDING, CONFIRMED, REJECTED, CANCELLED, EXPIRED, COMPLETED. Only PENDING and CONFIRMED occupy a future interval; a PENDING record at its start expires before conflict decisions.

## Closure

Stores room, start/end timestamp, reason (1-200), manager, createdAt and active flag. It may cross dates. An active closure blocks and cancels overlapping unfinished reservations after preview confirmation.

## Notification

Stores recipient, type, related business id, content, createdAt and readAt. Notifications are in-app and do not depend on subscription authorization.

## OperationLog

Stores operator, object type/id, action, result, summary and createdAt. Passwords and session credentials are never stored.

## Relationships and invariants

- User 1:N IdentityReview, Reservation, Notification, OperationLog.
- Room 1:N Reservation and Closure.
- Overlap: `new.start < existing.end AND new.end > existing.start`, same room only.
- Every state transition is conditional on the current state and timestamp; terminal states are immutable.
- `Reservation.requestId + applicantId` is unique for idempotent writes.
