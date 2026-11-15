from datetime import date
from uuid import uuid4

from playwright.sync_api import sync_playwright


BASE_URL = "http://127.0.0.1:8080/api/v1/"
TEST_DATE = "2027-01-15"


def expect_status(response, expected, label):
    actual = response.status
    if actual != expected:
        raise AssertionError(f"{label}: expected HTTP {expected}, got {actual}: {response.text()}")


def json(response, expected, label):
    expect_status(response, expected, label)
    return response.json()


def main():
    checks = []
    with sync_playwright() as playwright:
        request = playwright.request.new_context(base_url=BASE_URL)
        student = {"X-User-Id": "student-demo"}
        teacher = {"X-User-Id": "teacher-demo"}
        manager = {"X-User-Id": "manager-demo"}

        rooms = json(request.get("rooms", headers=student), 200, "list rooms")
        assert len(rooms) == 5, f"expected 5 rooms, got {len(rooms)}"
        assert {item["roomNo"] for item in rooms} == {"7060", "7081", "7091", "7119", "7052"}
        checks.append("room list returns five seeded rooms")

        schedule = json(request.get("rooms/7081/schedule", params={"date": TEST_DATE}, headers=student), 200, "empty schedule")
        assert isinstance(schedule, list)
        checks.append("schedule query succeeds")

        student_request_id = f"pw-student-{uuid4()}"
        student_payload = {
            "roomNo": "7081",
            "date": TEST_DATE,
            "start": "09:00",
            "end": "10:00",
            "reason": "Playwright接口验证",
            "attendeeCount": 4,
            "phone": "13800000001",
            "note": "",
            "requestId": student_request_id,
        }
        created = json(request.post("reservations", data=student_payload, headers=student), 201, "student reservation")
        assert created["status"] == "PENDING"
        reservation_id = created["id"]
        checks.append("student reservation creates PENDING")

        retried = json(request.post("reservations", data=student_payload, headers=student), 201, "idempotent retry")
        assert retried["id"] == reservation_id
        checks.append("same requestId returns the original reservation")

        teacher_payload = dict(student_payload)
        teacher_payload["phone"] = "13800000002"
        teacher_payload["requestId"] = f"pw-teacher-conflict-{uuid4()}"
        conflict = request.post("reservations", data=teacher_payload, headers=teacher)
        conflict_body = json(conflict, 409, "teacher conflict")
        assert conflict_body["code"] == "TIME_CONFLICT"
        checks.append("overlapping teacher reservation is rejected")

        public_schedule = json(request.get("rooms/7081/schedule", params={"date": TEST_DATE}, headers=teacher), 200, "public schedule privacy")
        own_entries = [entry for entry in public_schedule if entry.get("own")]
        other_entries = [entry for entry in public_schedule if not entry.get("own") and entry.get("status") == "PENDING"]
        assert not own_entries, "teacher should not see student's reservation as own"
        assert other_entries and other_entries[0]["reservationId"] is None
        checks.append("public schedule hides another user's reservation id")

        pending = json(request.get("admin/reservations", params={"status": "PENDING"}, headers=manager), 200, "manager pending ledger")
        assert any(item["id"] == reservation_id for item in pending)
        checks.append("manager can query pending ledger")

        approved = json(request.post(f"admin/reservations/{reservation_id}/approve", headers=manager), 200, "approve reservation")
        assert approved["status"] == "CONFIRMED"
        checks.append("manager approval changes reservation to CONFIRMED")

        cancelled = json(request.post(f"reservations/{reservation_id}/cancel", data={"reason": "Playwright清理"}, headers=student), 200, "cancel reservation")
        assert cancelled["status"] == "CANCELLED"
        checks.append("owner can cancel before start")

        teacher_request_id = f"pw-teacher-{uuid4()}"
        teacher_payload = {
            "roomNo": "7091",
            "date": TEST_DATE,
            "start": "11:00",
            "end": "12:00",
            "reason": "教师接口验证",
            "attendeeCount": 2,
            "phone": "13800000002",
            "note": "",
            "requestId": teacher_request_id,
        }
        teacher_created = json(request.post("reservations", data=teacher_payload, headers=teacher), 201, "teacher reservation")
        assert teacher_created["status"] == "CONFIRMED"
        teacher_cancelled = json(request.post(f"reservations/{teacher_created['id']}/cancel", data={"reason": "Playwright清理"}, headers=teacher), 200, "teacher cancel")
        assert teacher_cancelled["status"] == "CANCELLED"
        checks.append("teacher reservation creates CONFIRMED and can be cancelled")

        notifications = json(request.get("notifications", headers=student), 200, "notifications")
        assert isinstance(notifications, list)
        checks.append("notification list succeeds")

        print("PASS")
        for check in checks:
            print(f"- {check}")
        request.dispose()


if __name__ == "__main__":
    main()
