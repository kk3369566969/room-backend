# 会议室预约系统后端说明

## 1. 项目定位

这是面向华中师范大学计算机学院师生和办公室负责人的 Spring Boot REST 后端。首期覆盖身份审核、五间会议室查询、师生预约、负责人确认/拒绝、停用影响处理、通知、生命周期状态和操作日志。

不包含微信小程序页面、真实微信登录、学校统一身份认证、订阅消息、付费、门禁、签到、周期预约或数据迁移。首期用人工身份审核完成业务闭环，不保存学校密码。

## 2. 技术与目录

- Java 17
- Spring Boot 3.3.5 / Spring MVC
- Spring Data JPA / Hibernate
- MySQL 8
- Flyway 数据库迁移
- Jakarta Bean Validation

主要目录：

```text
src/main/java/cn/ccnu/roombooking/
├── common/       错误结构、请求身份和 DTO
├── config/       X-User-Id 开发身份过滤器
├── domain/       实体、枚举、仓储和锁定查询
├── service/      事务业务规则和生命周期处理
└── web/          REST 控制器
src/main/resources/db/migration/V1__init.sql
docs/api.md
docs/openapi.yaml
```

## 3. 本地运行

先创建数据库：

```sql
CREATE DATABASE room_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

默认连接参数是：`localhost:3306`、数据库 `room_booking`、用户名 `root`、密码 `1234`。可通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 覆盖。

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

开发联调在每个请求添加 `X-User-Id`。生产环境应替换 `AuthConfig` 中的请求头适配器为真实会话或微信身份校验，并保留服务端角色判断。

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

数据库表结构见 `src/main/resources/db/migration/V1__init.sql`。日志不保存密码和会话凭证。正式部署需启用 HTTPS、替换开发身份适配器、限制数据库账号权限、配置备份和确认学院的数据保存期限。

## 6. 验证

接口清单和示例见 [docs/api.md](api.md)，前端页面、状态和 AIGC 生成约束见 [docs/frontend-integration-guide.md](frontend-integration-guide.md)，机器可读契约见 [docs/openapi.yaml](openapi.yaml)，Speckit 使用过程见 [docs/speckit-usage.md](speckit-usage.md)，验证步骤见 `specs/001-meeting-room-booking/quickstart.md`。
