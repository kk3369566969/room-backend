# 会议室预约系统前端对接与 AIGC 生成说明

## 1. 文档用途

本文档给前端开发者和 AIGC 生成工具使用。目标前端是微信小程序，也可以按同一接口模型生成 Web 管理端。前端只负责展示、输入和交互提示，用户身份、角色、预约状态、时间和冲突结果以服务端响应为准。

后端接口完整说明见 [`api.md`](api.md)，机器可读契约见 [`openapi.yaml`](openapi.yaml)。

## 2. 接入约定

### 2.1 环境配置

| 环境 | Base URL | 说明 |
|---|---|---|
| 本地开发 | `http://localhost:8080/api/v1` | 微信开发者工具或 Web 本地联调 |
| 测试环境 | 由部署环境提供 | 不要把地址写死在页面代码中 |
| 生产环境 | HTTPS 地址 | 必须替换开发身份适配器 |

生产环境先调用微信登录接口取得 Bearer 令牌，再在每次请求中添加 `Authorization: Bearer <accessToken>`。当前后端仍保留 `X-User-Id` 供本地开发联调：

- `student-demo`：已核验学生
- `teacher-demo`：已核验教师
- `manager-demo`：已核验教师并拥有负责人权限

真实小程序接入时，将 `X-User-Id` 替换为登录态令牌或服务端会话，不要让用户在页面上选择或修改角色。

微信登录流程：

1. 调用 `wx.login()` 获取临时 `code`。
2. `POST /auth/wechat-login`，请求体为 `{ "code": "..." }`。
3. 保存响应中的 `accessToken`，后续请求添加 `Authorization: Bearer <accessToken>`。
4. `registered=false` 时进入身份提交页，调用 `POST /identity/reviews`。
5. 退出登录调用 `POST /auth/logout`，成功后清除本地令牌。

### 2.2 HTTP 客户端行为

统一封装 `request()`：

1. 自动拼接 Base URL。
2. 自动添加身份凭证和 `Content-Type: application/json`。
3. 对 `2xx` 返回 JSON 数据。
4. 对 `400/401/403/404/409/500` 解析 `{ code, message, details, timestamp }`。
5. 网络超时或请求结果不确定时，预约提交必须保留原 `requestId` 重试。
6. 不要因为按钮隐藏而跳过服务端错误处理。

建议的前端错误处理：

| code | 页面处理 |
|---|---|
| `UNAUTHORIZED` | 清理登录态并回到登录/身份页 |
| `IDENTITY_REQUIRED` | 打开身份提交或审核结果页 |
| `MANAGER_REQUIRED` | 显示无管理权限，不展示管理数据 |
| `VALIDATION_ERROR` | 将 `details` 映射到具体表单字段 |
| `TIME_CONFLICT` | 刷新日程并提示用户重新选择 |
| `ROOM_CLOSED` | 刷新日程并标记停用时段 |
| `CAPACITY_EXCEEDED` | 回到人数输入并展示当前容量 |
| `START_IN_PAST` / `ALREADY_STARTED` | 刷新服务端时间和预约详情 |
| `RESERVATION_HANDLED` | 刷新详情，展示最新状态 |
| `FORBIDDEN` | 显示无权访问，不重试同一请求 |

## 3. 前端状态模型

### 3.1 用户身份

```text
UNVERIFIED -> PENDING -> APPROVED
                         -> REJECTED -> PENDING
APPROVED -> REVOKED
```

页面文案：

- `UNVERIFIED`：提交身份资料
- `PENDING`：身份审核中，暂不能查询日程和预约
- `APPROVED`：已核验，可按 `STUDENT` 或 `TEACHER` 使用业务
- `REJECTED`：显示拒绝原因，允许重新提交
- `REVOKED`：资格已撤销，联系负责人

### 3.2 预约状态

```text
PENDING -> CONFIRMED
        -> REJECTED
        -> CANCELLED
        -> EXPIRED
CONFIRMED -> CANCELLED
           -> COMPLETED
```

状态文案必须使用：`待确认`、`预约成功`、`已拒绝`、`已取消`、`已失效`、`已完成`。不要把学生的 `PENDING` 显示成“预约成功”，也不要把教师的 `CONFIRMED` 显示成“待审批”。

## 4. 页面与接口映射

### 4.1 登录 / 身份提交页

展示：姓名、学工号、师生类型、手机号、提交按钮、字段错误。

接口：

- `POST /auth/wechat-login`
- `POST /identity/reviews`
- `GET /identity/me`

交互规则：

- 首次提交显示提交中状态，防止重复点击。
- `employeeNo` 已绑定时只提示“学工号不可用”，不展示已绑定账号。
- 提交成功跳转身份结果页，显示 `PENDING`。

### 4.2 身份结果页

展示当前身份状态、拒绝原因、重新提交入口和进入首页的条件。

接口：

- `POST /auth/wechat-login`
- `GET /identity/me`
- `POST /identity/reviews`

只有 `APPROVED` 才显示房间查询入口；`PENDING`、`REJECTED`、`REVOKED` 不允许前端绕过状态进入预约。

### 4.3 房间列表页

展示五间固定房间：房间号、地点、容量、设备、资料状态。

接口：

- `GET /rooms`

交互规则：

- `capacity=null` 显示“容量待核实”，不要显示为0或无限容量。
- `equipment` 或限制信息为“待核实”时保持原文。
- 加载失败提供重试按钮；空列表和网络失败要区分展示。

### 4.4 房间详情 / 日程页

展示：日期切换、08:00—22:00时间轴、空闲、已占用、停用、非开放和本人记录。

接口：

- `GET /rooms/{roomNo}/schedule?date=YYYY-MM-DD`

交互规则：

- `own=false` 的记录只显示统一“已占用”，不能尝试读取申请人信息。
- `own=true` 可以进入本人预约详情。
- 只允许点击空闲时段进入预约表单；提交前仍需再次请求服务端。
- 相邻时段允许选择，不要在前端自行增加缓冲时间。

### 4.5 预约表单页

字段：房间号、日期、开始时间、结束时间、理由、预计人数、手机号、备注。

接口：

- `POST /reservations`

前端校验：

- 理由为1—200字，不能只有空格。
- 预计人数为正整数。
- 手机号按 `1[3-9]\\d{9}` 做格式提示。
- 时间为同日 `08:00 <= start < end <= 22:00`。
- 不设置提前7天限制，不在客户端判断“必须提前几天”。

提交实现：

1. 页面进入时生成一个 UUID 作为 `requestId`。
2. 点击提交后立即禁用按钮。
3. 网络超时或结果不确定时，用同一个 `requestId` 查询/重试，不生成新编号。
4. `PENDING` 显示“已提交，等待负责人确认”；`CONFIRMED` 显示“预约成功”。
5. `409` 冲突后刷新日程，不保留过期的可选时段。

### 4.6 我的预约与预约详情

接口：

- `GET /reservations`
- `GET /reservations/{id}`
- `POST /reservations/{id}/cancel`

交互规则：

- 列表支持按状态和日期筛选，详情展示状态、原因和时间信息。
- `PENDING` 或 `CONFIRMED` 且未开始时显示取消按钮。
- 取消必须二次确认；成功后刷新详情和日程。
- 开始后隐藏自助取消按钮，但仍要处理服务端 `ALREADY_STARTED`。
- 改期或换房跳转到新建预约流程，先取消原记录，不承诺原时段保留。

### 4.7 通知中心

接口：

- `GET /notifications`
- `POST /notifications/{id}/read`

展示身份审核、确认、拒绝、取消、失效和停用取消通知。未授权订阅消息不影响站内通知展示。

### 4.8 负责人端

负责人入口必须由当前用户状态和后端权限共同决定，不允许通过修改前端路由直接进入。

接口：

- `GET /identity/reviews`
- `POST /identity/reviews/{id}/decision`
- `GET /admin/reservations?status=...`
- `POST /admin/reservations/{id}/approve`
- `POST /admin/reservations/{id}/reject`
- `PUT /admin/rooms/{roomNo}`
- `POST /admin/closures/preview`
- `POST /admin/closures`
- `POST /admin/closures/{id}/release`
- `GET /admin/logs`

停用流程必须分两步：填写房间、开始、结束和原因 -> 预览影响 -> 用户确认 -> 创建停用并统一处理。预览结果变化时，以服务端最终响应为准并要求重新确认。

## 5. 推荐前端目录

```text
src/
├── api/
│   ├── client.ts              # request 封装、错误转换、身份头
│   ├── identity.ts
│   ├── rooms.ts
│   ├── reservations.ts
│   ├── notifications.ts
│   └── admin.ts
├── pages/
│   ├── identity-submit/
│   ├── identity-result/
│   ├── rooms/
│   ├── room-schedule/
│   ├── reservation-form/
│   ├── my-reservations/
│   ├── reservation-detail/
│   ├── notifications/
│   └── admin/
├── components/
│   ├── status-badge/
│   ├── time-grid/
│   ├── error-state/
│   └── confirm-dialog/
└── store/
    ├── session
    ├── identity
    └── reservation
```

## 6. 可直接交给 AIGC 的生成提示词

```text
你是一个负责交付的前端工程师。请基于以下后端契约生成会议室预约系统前端，目标平台为微信小程序，代码使用 TypeScript。

后端地址：{BASE_URL}/api/v1
身份请求头：X-User-Id: {USER_ID}
接口文档：docs/api.md
OpenAPI：docs/openapi.yaml

必须实现页面：
1. 身份提交页
2. 身份审核结果页
3. 房间列表页
4. 房间详情和日程页
5. 预约表单页
6. 我的预约页
7. 预约详情页
8. 通知中心
9. 负责人身份审核、申请处理、房间维护、停用预览和台账页

必须遵守的业务规则：
- 未核验用户不能查询日程或预约。
- 学生提交后显示“待确认”并立即占用；教师提交后显示“预约成功”。
- 待确认、预约成功和停用区间阻止重叠预约。
- 时间必须是同日08:00—22:00，精确到分钟，相邻时段允许。
- 提交请求必须生成并复用 requestId，防止网络重试重复登记。
- 公共日程不能展示他人姓名、手机号、理由和审核内容。
- 所有状态和权限以服务端响应为准，不能由客户端指定角色或状态。

请先输出：页面路由、状态模型、接口调用表和组件拆分；再生成完整代码。所有加载中、空列表、网络失败、401、403、409、表单错误和重复点击状态都要有可见处理。不要虚构后端没有返回的会议室容量、设备或用户信息。
```

## 7. 前端验收清单

- [ ] 未核验用户无法进入房间日程和预约流程。
- [ ] 五间房间和未知资料展示正确。
- [ ] 他人预约在日程中只有统一占用状态。
- [ ] 学生和教师提交结果文案不同且与状态一致。
- [ ] 同一 `requestId` 重试不会创建第二条记录。
- [ ] 冲突、停用、超容量和非法时间都有明确提示。
- [ ] 开始前可取消，开始后不显示或拒绝自助取消。
- [ ] 负责人操作前显示影响预览，拒绝原因和停用原因必填。
- [ ] 通知支持未读、已读和业务详情跳转。
- [ ] 小屏、键盘弹出、长理由和重复点击不造成布局或数据错误。
