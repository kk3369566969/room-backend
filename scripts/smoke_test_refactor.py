"""重构后的端到端冒烟测试：覆盖登录态、预约、审批、停用、通知与日志。"""
import json
import time
import urllib.request
import urllib.error
from datetime import date, timedelta

BASE = "http://127.0.0.1:8080/api/v1"
STUDENT = {"X-User-Id": "student-demo"}
TEACHER = {"X-User-Id": "teacher-demo"}
MANAGER = {"X-User-Id": "manager-demo"}
RUN = str(int(time.time()))[-6:]
# 每次运行换一天，避免上次残留数据干扰
DAY = (date(2028, 1, 1) + timedelta(days=int(RUN) % 300)).isoformat()


def new_request_id(name):
    return f"smoke-{RUN}-{name}"

passed = []


def _decode(raw):
    if not raw:
        return None
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return raw


def call(method, path, headers=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    for key, value in (headers or {}).items():
        req.add_header(key, value)
    if data:
        req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, _decode(resp.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, _decode(e.read().decode())


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected}, got {actual}"
    passed.append(label)


# 1. 未认证：开发模式下不带请求头会落到默认演示账号，因此这里用一个不存在的身份验证 401 分支
ANONYMOUS = {"X-User-Id": "no-such-user"}
status, body = call("GET", "/rooms", ANONYMOUS)
check("未知身份访问会议室返回 401", status, 401)
check("未知身份错误码", body["code"], "UNAUTHORIZED")

# 2. 身份接口
status, me = call("GET", "/identity/me", STUDENT)
check("查询当前身份", (status, me["name"]), (200, "演示学生"))

# 3. 创建预约（学生 -> PENDING）
payload = {
    "roomNo": "7091", "date": DAY, "start": "14:00", "end": "15:30",
    "reason": "重构冒烟测试", "attendeeCount": 6, "phone": "13800000001",
    "note": "备注", "requestId": new_request_id("001"),
}
status, created = call("POST", "/reservations", STUDENT, payload)
check("学生预约创建", (status, created["status"]), (201, "PENDING"))
check("预约时段回显", (created["start"], created["end"]), ("14:00", "15:30"))
check("预约会议室号", created["roomNo"], "7091")
rid = created["id"]

# 4. 幂等
status, again = call("POST", "/reservations", STUDENT, payload)
check("重复 requestId 返回原预约", (status, again["id"]), (201, rid))

# 5. 时段冲突
conflict_payload = dict(payload, requestId=new_request_id("002"), start="15:00", end="16:00")
status, conflict = call("POST", "/reservations", TEACHER, conflict_payload)
check("时段冲突返回 409", status, 409)
check("冲突错误码", conflict["code"], "TIME_CONFLICT")

# 6. 容量校验
over = dict(payload, requestId=new_request_id("003"), start="16:00", end="17:00", attendeeCount=20)
status, capacity = call("POST", "/reservations", TEACHER, over)
check("超容量返回 400", (status, capacity["code"]), (400, "CAPACITY_EXCEEDED"))

# 7. 日程可见性
status, mine_schedule = call("GET", f"/rooms/7091/schedule?date={DAY}", STUDENT)
own = [i for i in mine_schedule if i["reservationId"] == rid]
check("本人日程带预约号", len(own), 1)
status, other_schedule = call("GET", f"/rooms/7091/schedule?date={DAY}", TEACHER)
hidden = [i for i in other_schedule if i["status"] == "PENDING"]
check("他人日程隐藏预约号", hidden[0]["reservationId"], None)
check("他人日程 own=false", hidden[0]["own"], False)

# 8. 详情权限
status, _ = call("GET", f"/reservations/{rid}", TEACHER)
check("越权查看详情返回 403", status, 403)
status, detail = call("GET", f"/reservations/{rid}", STUDENT)
check("本人详情含申请人", (status, detail["applicantName"]), (200, "演示学生"))

# 9. 普通用户访问管理端
status, forbidden = call("GET", "/admin/logs", STUDENT)
check("非负责人访问管理端 403", (status, forbidden["code"]), (403, "MANAGER_REQUIRED"))

# 10. 审批
status, approved = call("POST", f"/admin/reservations/{rid}/approve", MANAGER)
check("负责人确认预约", (status, approved["status"]), (200, "CONFIRMED"))

# 11. 台账
status, ledger = call("GET", f"/admin/reservations?status=CONFIRMED", MANAGER)
check("台账按状态筛选", any(item["id"] == rid for item in ledger), True)

# 12. 通知 + 已读
status, notes = call("GET", "/notifications", STUDENT)
check("学生收到通知", (status, isinstance(notes, list)), (200, True))
status, read = call("POST", f"/notifications/{notes[0]['id']}/read", STUDENT)
check("通知标记已读", (status, read["readAt"] is not None), (200, True))

# 13. 取消
status, cancelled = call("POST", f"/reservations/{rid}/cancel", STUDENT, {"reason": "测试结束"})
check("自助取消", (status, cancelled["status"]), (200, "CANCELLED"))
status, again_cancel = call("POST", f"/reservations/{rid}/cancel", STUDENT, {"reason": "重复取消"})
check("重复取消幂等", (status, again_cancel["status"]), (200, "CANCELLED"))

# 14. 停用预览 / 创建 / 释放
teacher_payload = dict(payload, requestId=new_request_id("004"), start="19:00", end="20:00")
status, teacher_created = call("POST", "/reservations", TEACHER, teacher_payload)
check("教师预约直接确认", teacher_created["status"], "CONFIRMED")

closure = {"roomNo": "7091", "startAt": f"{DAY}T18:00:00", "endAt": f"{DAY}T21:00:00",
           "reason": "设备维护", "confirm": True}
status, preview = call("POST", "/admin/closures/preview", MANAGER, closure)
check("停用预览命中 1 条", (status, len(preview)), (200, 1))
status, closure_id = call("POST", "/admin/closures", MANAGER, closure)
check("创建停用", (status, len(closure_id)), (201, 36))
status, released = call("POST", f"/admin/closures/{closure_id}/release", MANAGER)
check("解除停用", (status, released["active"]), (200, False))
check("停用响应含会议室", released["room"]["roomNo"], "7091")
check("停用响应含负责人", released["manager"]["externalId"], "manager-demo")

status, affected = call("GET", f"/reservations/{teacher_created['id']}", TEACHER)
check("停用后预约被取消", affected["status"], "CANCELLED")

# 15. 房间资料更新
status, room = call("PUT", "/admin/rooms/7052", MANAGER, {"capacity": 40, "location": "综合楼七楼东"})
check("更新房间资料", (status, room["capacity"], room["location"]), (200, 40, "综合楼七楼东"))
call("PUT", "/admin/rooms/7052", MANAGER, {"capacity": 35, "location": "综合楼七楼"})

# 16. 日志
status, logs = call("GET", "/admin/logs", MANAGER)
check("操作日志返回", (status, isinstance(logs, list)), (200, True))
check("日志含操作人", logs[0]["operator"]["externalId"], "manager-demo")
check("日志按时间倒序", logs[0]["createdAt"] >= logs[-1]["createdAt"], True)

# 17. 身份提交（新用户）
status, submitted = call("POST", "/identity/reviews", {"X-User-Id": "new-user-smoke"},
                         {"name": "新同学", "employeeNo": f"S-SMOKE-{RUN}",
                          "memberType": "STUDENT", "phone": "13800000009"})
check("提交身份资料", (status, submitted["status"]), (201, "PENDING"))
status, pending = call("GET", "/admin/reviews", MANAGER)
check("待审核列表含新提交", any(r["employeeNoSnapshot"] == f"S-SMOKE-{RUN}" for r in pending), True)
review_id = [r["id"] for r in pending if r["employeeNoSnapshot"] == f"S-SMOKE-{RUN}"][0]
status, rejected = call("POST", f"/identity/reviews/{review_id}/decision", MANAGER,
                        {"approved": False, "reason": "资料无法核实"})
check("拒绝身份审核", (status, rejected["status"]), (200, "REJECTED"))
check("审核记录带审核人", rejected["reviewer"]["externalId"], "manager-demo")

# 18. 参数校验
status, invalid = call("POST", "/reservations", STUDENT,
                       dict(payload, requestId=new_request_id("005"), start="07:00"))
check("非开放时段 400", (status, invalid["code"]), (400, "INVALID_INTERVAL"))
status, bad_time = call("POST", "/reservations", STUDENT,
                        dict(payload, requestId=new_request_id("006"), start="25:99"))
check("非法时间格式 400", (status, bad_time["code"]), (400, "INVALID_TIME"))

print(f"\n全部 {len(passed)} 项断言通过")
for item in passed:
    print("  ✓", item)
