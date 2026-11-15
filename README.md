# 会议室预约系统后端

面向学院师生和会议室负责人的会议室预约 REST 后端。系统支持身份审核、会议室与日程查询、学生待确认预约、教师直接预约、冲突控制、负责人处理、会议室停用、站内通知和操作日志。

## 项目状态

- Java 17 + Spring Boot 3.3.5
- MySQL 8 + Flyway
- REST API 前缀：`/api/v1`
- 当前使用 `X-User-Id` 作为开发环境身份适配器
- 不包含微信小程序页面、真实微信登录、统一身份认证和生产部署配置

## 核心能力

- 身份资料提交、负责人审核和重新提交
- 五间会议室：7060、7081、7091、7119、7052
- 按日期查询公共日程，并隐藏其他用户的个人信息
- 学生预约进入 `PENDING`，教师预约直接进入 `CONFIRMED`
- 同房间重叠预约、停用区间和容量校验
- `requestId` 幂等提交，支持网络重试不重复登记
- 负责人确认/拒绝、停用影响预览、停用释放和管理台账
- 预约失效、完成、取消等生命周期处理
- 站内通知和操作日志

## 快速开始

### 1. 准备环境

- JDK 17+
- Maven 3.9+
- MySQL 8+

创建数据库：

```sql
CREATE DATABASE room_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 配置数据库

默认连接配置：

| 配置项 | 默认值 |
|---|---|
| 地址 | `localhost:3306` |
| 数据库 | `room_booking` |
| 用户名 | `root` |
| 密码 | `1234` |

可以通过环境变量覆盖默认值：

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/room_booking?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='1234'
```

### 3. 启动服务

```powershell
mvn spring-boot:run
```

服务启动后地址为 `http://localhost:8080`。Flyway 会自动执行数据库迁移，并初始化会议室和演示账号。

## 开发身份

开发联调时，在每个请求中添加 `X-User-Id`：

| 请求头值 | 身份 | 权限 |
|---|---|---|
| `student-demo` | 已核验学生 | 查询、提交待确认预约、取消本人预约 |
| `teacher-demo` | 已核验教师 | 查询、直接预约成功、取消本人预约 |
| `manager-demo` | 已核验教师 + 负责人 | 身份审核、预约处理、停用和台账 |

示例：

```powershell
curl.exe -H "X-User-Id: student-demo" http://localhost:8080/api/v1/rooms
```

生产环境必须替换 `AuthConfig` 中的开发身份适配器，使用真实会话或微信身份校验；角色和预约状态始终由服务端决定。

## 文档入口

| 文档 | 用途 |
|---|---|
| [docs/backend-guide.md](docs/backend-guide.md) | 后端架构、配置、数据库和部署说明 |
| [docs/api.md](docs/api.md) | 接口、请求示例、错误码和隐私规则 |
| [docs/openapi.yaml](docs/openapi.yaml) | OpenAPI 3.0 机器可读接口契约 |
| [docs/frontend-integration-guide.md](docs/frontend-integration-guide.md) | 前端页面、状态模型和 AIGC 生成提示 |
| [docs/speckit-usage.md](docs/speckit-usage.md) | 本项目使用 Speckit 的完整过程 |
| [specs/001-meeting-room-booking/spec.md](specs/001-meeting-room-booking/spec.md) | 需求规格和验收条件 |
| [specs/001-meeting-room-booking/plan.md](specs/001-meeting-room-booking/plan.md) | 技术计划和架构决策 |
| [specs/001-meeting-room-booking/tasks.md](specs/001-meeting-room-booking/tasks.md) | 实现任务清单 |

## 验证

编译项目：

```powershell
mvn -q -DskipTests compile
```

仓库提供了 Playwright API 验证脚本，覆盖房间列表、日程、学生/教师预约、幂等、冲突、负责人确认、取消和通知：

```powershell
# 先启动后端，再运行脚本
$env:NODE_PATH='C:\path\to\node_modules'
node scripts/test_backend_api.mjs
```

脚本使用测试日期和 `pw-*` 请求标识。运行后请清理测试数据库中的测试预约；不要在生产数据库执行该脚本。

## 目录结构

```text
room-backend/
├── src/main/java/cn/ccnu/roombooking/
│   ├── common/       DTO、错误处理、身份上下文和时间规则
│   ├── config/       开发身份过滤器
│   ├── domain/       实体、枚举和仓储
│   ├── service/      事务业务规则和生命周期处理
│   └── web/          REST 控制器
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/ Flyway 迁移脚本
├── docs/              后端、接口、前端和 Speckit 文档
├── specs/             需求、计划、数据模型、契约和任务
└── scripts/           接口验证脚本
```

## 关键规则

- 只有身份状态为 `APPROVED` 的用户可以查询日程和提交预约。
- 预约时间必须为同一天的 `08:00—22:00`，开始时间晚于服务端当前时间。
- 重叠判断为：`新开始 < 已有结束 && 新结束 > 已有开始`；相邻时段允许。
- 学生的 `PENDING` 记录立即占用时段，教师的 `CONFIRMED` 记录直接占用时段。
- 公共日程不返回其他用户的姓名、手机号、理由或审核信息。
- 停用必须先预览影响，确认后取消重叠且尚未结束的预约；解除停用不会恢复已取消记录。
- 数据库中不保存密码和会话凭证。

## 生产前事项

- 接入真实登录和会话校验，移除或限制 `X-User-Id` 开发身份模式。
- 启用 HTTPS、数据库最小权限和敏感配置管理。
- 核实 7060 的容量、设备和特殊限制。
- 根据学院要求确定个人信息保存期限、备份策略和恢复演练方案。
- 完成并发、性能、越权和恢复目标的正式验收。
