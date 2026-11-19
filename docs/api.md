# 会议室预约系统接口文档

Base URL：`http://localhost:8080/api/v1`。生产登录使用微信小程序 `code` 换取服务端 Bearer 会话令牌；本地开发默认免登录（开发模式开启时未带凭证的请求以 `manager-demo` 身份执行，可用 `X-User-Id` 切换身份）。请求和响应使用 JSON，时间按北京时间处理。

## 微信登录

### `POST /auth/wechat-login`

小程序端先调用 `wx.login()`，将返回的临时 `code` 交给后端。后端调用微信 `jscode2session` 获取 `openid`，创建服务端会话，并返回访问令牌。后续请求统一添加：

```http
Authorization: Bearer <accessToken>
```

请求体：

```json
{"code":"微信 wx.login 返回的临时 code"}
```

返回 `200`：

```json
{
  "accessToken":"服务端随机令牌",
  "tokenType":"Bearer",
  "expiresIn":604800,
  "externalId":"微信 openid",
  "registered":false,
  "identityStatus":null,
  "memberType":null
}
```

`registered=false` 表示该微信用户还没有提交身份资料。首次使用时，前端在拿到令牌后调用 `POST /identity/reviews` 提交身份资料。

微信配置使用环境变量 `WECHAT_APP_ID`、`WECHAT_APP_SECRET`，可通过 `WECHAT_JSCODE2SESSION_URL` 指定测试环境的微信接口地址。未配置时返回 `WECHAT_NOT_CONFIGURED`。

### `POST /auth/logout`

需要 Bearer 令牌。服务端撤销当前会话，返回 `204 No Content`。

## 统一错误

```json
{
  "code": "TIME_CONFLICT",
  "message": "该时段已被占用",
  "details": null,
  "timestamp": "2026-11-15T10:00:00+08:00"
}
```

常见错误码：`UNAUTHORIZED`、`IDENTITY_REQUIRED`、`MANAGER_REQUIRED`、`VALIDATION_ERROR`、`TIME_CONFLICT`、`ROOM_CLOSED`、`CAPACITY_EXCEEDED`、`START_IN_PAST`、`ALREADY_STARTED`、`RESERVATION_HANDLED`、`FORBIDDEN`。

## 身份审核

### `POST /identity/reviews`

提交身份资料。需要先完成微信登录。字段：`name` 1—50字、`employeeNo` 1—30字符、`memberType` 为 `STUDENT` 或 `TEACHER`、`phone` 为中国大陆11位号码。

```json
{"name":"张三","employeeNo":"20260001","memberType":"STUDENT","phone":"13800000001"}
```

返回 `201`：

```json
{"id":"uuid","status":"PENDING","message":"身份资料已提交"}
```

### `GET /identity/reviews`

负责人查看待审核身份。普通用户返回 `403`。

### `POST /identity/reviews/{id}/decision`

负责人审核：`{"approved":true}` 通过；拒绝时 `approved=false` 且 `reason` 必填，长度1—200字。

## 房间与日程

### `GET /rooms`

返回五间房间，未知容量或设备使用 `待核实`。

### `GET /rooms/{roomNo}/schedule?date=2026-12-01`

返回该日期日程。示例：

```json
[{"reservationId":null,"start":"09:00","end":"10:00","status":"PENDING","own":false}]
```

他人记录的 `reservationId` 为空，`own=false`，不返回姓名、手机号、理由或审核内容；本人记录返回编号和 `own=true`。

## 预约

### `POST /reservations`

学生和教师共用请求体，服务端从已核验身份决定结果，客户端不能指定状态或角色：

```json
{
  "roomNo":"7081",
  "date":"2026-12-01",
  "start":"09:00",
  "end":"10:30",
  "reason":"课程讨论",
  "attendeeCount":8,
  "phone":"13800000001",
  "note":"需要投影仪",
  "requestId":"client-uuid-001"
}
```

学生返回 `201` 且 `status=PENDING`；教师返回 `201` 且 `status=CONFIRMED`。同一个用户重复 `requestId` 返回原记录，不重复写入；冲突返回 `409 TIME_CONFLICT`。

### `GET /reservations`

查看本人预约历史，按日期倒序。列表不返回他人信息。

### `GET /reservations/{id}`

查看本人详情；负责人可以查看管理详情。替换编号访问他人记录返回 `403`。

### `POST /reservations/{id}/cancel`

开始前取消待确认或预约成功记录：`{"reason":"临时调整"}`。已开始、终态或他人记录不能自助取消。

## 负责人管理

所有 `/admin` 接口需要 `manager-demo` 或真实负责人授权。

- `GET /admin/reservations?status=PENDING`：台账筛选；状态可为 `PENDING`、`CONFIRMED`、`REJECTED`、`CANCELLED`、`EXPIRED`、`COMPLETED`。
- `POST /admin/reservations/{id}/approve`：重新校验冲突、容量和停用后确认学生申请。
- `POST /admin/reservations/{id}/reject`：请求体 `{"reason":"资料无法核实"}`，拒绝原因必填。
- `PUT /admin/rooms/{roomNo}`：更新 `capacity`、`equipment`、`restrictionText`、`location`。
- `POST /admin/closures/preview`：请求体含 `roomNo`、`startAt`、`endAt`、`reason`、`confirm`，返回受影响预约。
- `POST /admin/closures`：`confirm=true` 后创建停用并取消受影响记录，生成通知和日志。
- `POST /admin/closures/{id}/release`：解除停用，不恢复已取消预约。
- `GET /admin/logs`：查看最近100条操作日志。

## 通知

- `GET /notifications`：查看本人站内通知。
- `POST /notifications/{id}/read`：标记本人通知已读。

通知类型包含身份结果、确认、拒绝、本人取消、自动失效和停用取消。订阅消息未授权不影响站内通知。

完整 OpenAPI 3.0 契约见 [openapi.yaml](openapi.yaml)。
