# 会议室预约系统后端说明

## 1. 项目定位

这是面向华中师范大学计算机学院师生和办公室负责人的 Spring Boot REST 后端。首期覆盖身份审核、五间会议室查询、师生预约、负责人确认/拒绝、停用影响处理、通知、生命周期状态和操作日志。

不包含微信小程序页面、学校统一身份认证、订阅消息、付费、门禁、签到、周期预约或数据迁移。后端已支持微信小程序 `wx.login` 的 code 换取 Bearer 会话，身份资料仍由负责人人工审核，不保存学校密码。

## 2. 技术与目录

- Java 17
- Spring Boot 3.3.5 / Spring MVC
- MyBatis-Plus 3.5.9
- MySQL 8
- Flyway 数据库迁移
- Jakarta Bean Validation

主要目录：

```text
src/main/java/cn/ccnu/roombooking/
├── controller/   REST 控制器
├── service/      业务接口层，声明能力并附 Javadoc 契约
├── service/impl/ 业务实现层，事务边界与规则细节
├── mapper/       MyBatis-Plus Mapper（继承 BaseMapper）
├── entity/       表映射实体
├── dto/          request / response
├── enums/        枚举
├── config/       MyBatis-Plus 与配置属性
├── security/     登录态与鉴权
├── client/       外部服务客户端
└── common/       统一错误与时间规则
src/main/resources/mapper/ReservationMapper.xml
src/main/resources/db/migration/V1__init.sql
docs/api.md
docs/openapi.yaml
```

## 3. 本地运行

先创建数据库：

```sql
CREATE DATABASE room_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

默认连接参数是：`localhost:3306`、数据库 `room_booking`、用户名 `root`、密码 `1234`。可通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 覆盖。微信登录还需要配置 `WECHAT_APP_ID`、`WECHAT_APP_SECRET`，会话有效期可通过 `AUTH_SESSION_TTL_SECONDS` 覆盖。

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/room_booking?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='1234'
mvn spring-boot:run
```

Flyway 首次启动会创建表并写入五间房间和演示账号：

| X-User-Id | 类型 | 用途 |
|---|---|---|
| `student-demo` | 已核验学生 | 查询、提交待确认预约、取消本人预约 |
| `teacher-demo` | 已核验教师 | 查询、直接预约成功、取消本人预约 |
| `manager-demo` | 已核验教师 + 负责人授权 | 审核身份、处理学生申请、停用和台账 |

本地默认**免登录**：`app.auth.dev-mode=true` 时，未带任何凭证的请求自动以 `app.auth.dev-default-user`（默认 `manager-demo`）身份执行；带 `X-User-Id` 可临时切换成学生或教师。启动日志会打 WARN 提示。关闭方式：`AUTH_DEV_MODE=false`，或激活 `prod` / `production` profile（生产 profile 下即使配置为 true 也会被强制关闭并打 ERROR）。

生产小程序先调用 `POST /api/v1/auth/wechat-login`，将 `wx.login()` 返回的 code 换成 Bearer 令牌；后续请求添加 `Authorization: Bearer <accessToken>`。首次登录用户再调用 `POST /api/v1/identity/reviews`。开发身份在 `dev-mode` 关闭后完全失效，服务端角色判断始终保留。

## 4. 关键业务规则

- 只有 `APPROVED` 用户能查询日程和预约；负责人权限单独存储。
- 学生提交后状态为 `PENDING`，立即占用；教师提交后状态为 `CONFIRMED`。
- 同一房间同一天的重叠条件是 `新开始 < 已有结束 && 新结束 > 已有开始`，相邻时段允许。
- 时间必须在同日 08:00—22:00，开始时间晚于服务端当前时间；不限制提前七天。
- 已知容量超限拒绝；7060容量未知时仍要求人数，但不因容量未知拦截。
- 预约写入、负责人确认和取消在事务中完成，并使用重叠记录悲观锁；`申请人 + requestId` 保证重复提交幂等。
- 待确认到开始仍未处理会转为 `EXPIRED`，成功预约到结束转为 `COMPLETED`；查询时会触发生命周期校正。
- 停用先预览影响，确认后取消重叠且尚未结束的待确认/成功预约；解除停用不恢复已取消记录。
- 公共日程只返回占用状态；本人或负责人详情才返回必要的申请信息。

## 5. 数据与安全边界

数据库表结构见 `src/main/resources/db/migration/`，`V3__wechat_auth_session.sql` 保存会话令牌哈希和过期时间。日志不保存密码和明文会话凭证。正式部署需启用 HTTPS、限制数据库账号权限、配置备份和确认学院的数据保存期限。

## 6. 日志

- 落点：控制台 + `logs/room-booking.log`（20MB 滚动、保留 7 天、UTF-8）。
- 每行前缀为 `[traceId] [user]`。traceId 由 `common/log/TraceIdFilter` 生成（客户端带 `X-Trace-Id` 则复用），并用 MDC 透传到业务层日志；user 由 `AuthFilter` 解析后写入，未登录为 `anon`。
- 标记：`[访问]` 请求出入参耗时、`[业务异常]` 可预期拒绝、`[参数校验失败]`、`[登录]`、`[身份提交]`、`[身份审核]`、`[预约创建]`、`[预约取消]`、`[预约审批]`、`[状态结算]`、`[停用创建]`、`[停用解除]`、`[房间更新]`、`[微信登录]`。
- 级别：状态变更与成功调用 INFO；被拒绝的调用 WARN；未预期异常 ERROR（带堆栈）。
- 脱敏：手机号走 `Logs.maskPhone`，openid 等外部标识走 `Logs.abbrev`，令牌与 code 一律不打印。
- 追踪：同一 traceId 即可还原一次请求的全部日志；响应头 `X-Trace-Id` 会回传，便于前端报错时对齐。

## 7. 验证

接口清单和示例见 [docs/api.md](api.md)，前端页面、状态和 AIGC 生成约束见 [docs/frontend-integration-guide.md](frontend-integration-guide.md)，机器可读契约见 [docs/openapi.yaml](openapi.yaml)，Speckit 使用过程见 [docs/speckit-usage.md](speckit-usage.md)，验证步骤见 `specs/001-meeting-room-booking/quickstart.md`。
