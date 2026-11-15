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
| [docs/apifox-openapi.yaml](docs/apifox-openapi.yaml) | 可直接导入 Apifox 的 OpenAPI 文件 |
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
# 本项目 Speckit 使用过程说明

## 1. Speckit 的作用

Speckit 将自然语言需求拆成一组可追踪的设计和实现产物：

```text
需求文档
  -> spec.md
  -> plan.md + research.md + data-model.md + contracts/ + quickstart.md
  -> tasks.md
  -> Java 后端代码和文档
```

它解决的问题是：先明确用户价值和验收条件，再决定技术方案，最后按依赖顺序实现，避免直接从一份长需求文档跳到代码。

## 2. 本次项目使用的输入

用户提供的需求文件：`C:\Users\33695\Downloads\会议室预定系统_需求规格说明书.md`。

需求中的业务规则作为产品输入，例如：

- 只服务计算机学院师生和南湖校区综合楼七楼五间会议室。
- 学生提交后待负责人确认，教师提交后直接预约成功。
- 待确认记录也立即占用时段。
- 同一房间重叠预约必须由服务端原子阻止。
- 预约、取消、停用、通知和操作记录需要保留历史。

用户在聊天中的技术要求作为实现约束：

- 使用 Java 编写后端。
- 使用 MySQL，连接默认值为 `localhost:3306`、用户 `root`、密码 `1234`。
- 产出后端说明文档和接口文档。

两类输入要分开理解：需求文件描述产品规则，用户消息描述本次要完成的开发工作和技术选择。

## 3. 实际执行步骤

### 第一步：读取项目和 Speckit 配置

确认仓库只有 `.specify/` 结构，没有已有业务代码；读取：

- `.specify/init-options.json`
- `.specify/memory/constitution.md`
- Speckit 的 specify、plan、tasks、implement 技能说明

项目 constitution 仍是模板占位内容，因此没有额外的项目治理门禁；隐私、权限、审计和一致性规则来自需求文档。

### 第二步：生成规格说明

使用 `speckit-specify` 的流程，把需求整理成：

```text
specs/001-meeting-room-booking/spec.md
specs/001-meeting-room-booking/checklists/requirements.md
```

`spec.md` 主要内容：

- 五个用户故事，按 P1/P2 排序。
- 每个用户故事的独立测试方式和验收场景。
- 边界条件，例如同日时间、容量未知、重复提交和并发状态迁移。
- 16 条可测试功能需求。
- 关键实体和可衡量成功标准。
- 技术实现尚未写入业务验收条件，技术假设单独放在 Assumptions。

质量清单确认：没有未解决的 `[NEEDS CLARIFICATION]`，需求可测试，成功标准可衡量，范围和依赖明确。

### 第三步：生成实现计划

使用 `speckit-plan` 的流程生成：

```text
specs/001-meeting-room-booking/plan.md
specs/001-meeting-room-booking/research.md
specs/001-meeting-room-booking/data-model.md
specs/001-meeting-room-booking/contracts/openapi.yaml
specs/001-meeting-room-booking/quickstart.md
```

关键技术决策：

- Java 17 + Spring Boot 3.3。
- Spring Web、JPA、Bean Validation、MySQL Connector/J、Flyway、Lombok。
- MySQL 8 持久化，Flyway 管理版本迁移。
- 首期用 `X-User-Id` 作为开发身份适配器，生产环境可替换为微信或统一身份认证。
- 预约写入、确认、取消和停用在事务内执行，重叠查询使用锁。
- 公共日程和本人/负责人详情使用不同字段投影，保护隐私。

### 第四步：生成任务清单

使用 `speckit-tasks` 的流程生成：

```text
specs/001-meeting-room-booking/tasks.md
```

任务分为：

1. Setup：Maven、配置和入口类。
2. Foundational：数据库迁移、实体、仓储、错误处理和认证上下文。
3. US1：身份提交和审核。
4. US2：房间和日程查询。
5. US3：预约创建、冲突和取消。
6. US4：负责人处理和资源停用。
7. US5：通知、生命周期和台账。
8. Polish：后端说明、接口文档、OpenAPI 和 quickstart 验证。

每项任务都有编号、故事标签和实际文件路径。实现完成后将 30 项任务全部标记为 `[X]`。

### 第五步：执行实现

按照 `speckit-implement` 的依赖顺序实现：

- 先完成基础配置和数据库结构。
- 再实现服务端权限、领域状态和事务规则。
- 然后实现 REST 控制器和字段投影。
- 最后编写说明文档、接口文档和 OpenAPI。

实现结果位于：

```text
src/main/java/cn/ccnu/roombooking/
src/main/resources/application.yml
src/main/resources/db/migration/
docs/
```

### 第六步：验证

本次验证包括：

- `mvn -q -DskipTests compile` 编译通过。
- MySQL `localhost:3306` 使用 `root/1234` 连接成功。
- Flyway 初始化和后续迁移成功。
- Spring Boot 能连接数据库并启动 Tomcat。
- `GET /rooms` 返回五间房间。
- 学生预约返回 `PENDING`。
- 相同 `requestId` 重试返回同一条预约。
- 教师抢占同一时段返回 `TIME_CONFLICT`。

本项目没有因为用户未要求而新增测试套件；验收场景和 quickstart 仍作为后续测试依据。

## 4. 产物之间的关系

| 产物 | 作用 | 后续使用者 |
|---|---|---|
| `spec.md` | 产品范围、用户故事、验收条件 | 产品、分析、测试、AIGC |
| `plan.md` | 技术栈、结构和实现决策 | 后端开发 |
| `research.md` | 关键技术选择和备选方案 | 评审、维护者 |
| `data-model.md` | 实体字段、关系、状态和约束 | 后端、数据库、测试 |
| `contracts/openapi.yaml` | 机器可读接口契约 | 前端、接口工具、AIGC |
| `quickstart.md` | 从启动到核心流程的验证步骤 | 开发、测试、交接 |
| `tasks.md` | 依赖排序的可执行任务 | 实现代理、开发者 |
| `docs/frontend-integration-guide.md` | 前端页面、状态、接口和 AIGC 约束 | 前端、AIGC |

## 5. 后续需求变更怎么做

### 5.1 业务规则变化

例如“学生也可以直接预约”或“开放时间改为 07:00—23:00”：

1. 先更新 `spec.md` 的用户故事、功能需求、边界和验收场景。
2. 重新检查 `checklists/requirements.md`。
3. 更新 `plan.md`、`data-model.md` 和 `contracts/openapi.yaml` 中受影响内容。
4. 重新生成或修订 `tasks.md`，补充数据库迁移、服务和接口任务。
5. 实现任务并更新前端对接文档。
6. 重新执行 quickstart 和对应验收场景。

### 5.2 纯接口或字段变化

1. 更新 `contracts/openapi.yaml` 和 `docs/api.md`。
2. 检查 `docs/frontend-integration-guide.md` 的页面、字段、错误码和 AIGC 提示词。
3. 在 `tasks.md` 增加兼容性和迁移任务。
4. 编译并执行相关接口冒烟验证。

### 5.3 仅实现细节变化

如果用户价值、输入输出和验收条件不变，只需更新 `plan.md` 的研究决策和代码，不要无故修改 `spec.md` 的业务需求。

## 6. 常用命令顺序

在支持 Speckit 的项目环境中，后续可以按以下顺序使用：

```text
$speckit-specify 生成或更新用户需求规格
$speckit-clarify 处理仍存在的关键疑问
$speckit-plan    生成技术计划和设计产物
$speckit-tasks   生成依赖排序的实现任务
$speckit-implement 执行 tasks.md 中的实现任务
$speckit-analyze 检查 spec、plan、tasks 的一致性
$speckit-converge 对照产物和代码补齐遗漏任务
```

实际使用时，先确认当前 `.specify/feature.json` 指向目标特性目录，再执行后续阶段。一次只处理一个特性目录，避免多个需求共用同一份 `tasks.md`。

## 7. 当前项目的边界

当前产物已经覆盖后端核心闭环和前端生成所需契约，但以下内容仍需要正式部署前补充：

- 微信真实登录和会话校验。
- 学校统一身份认证是否获准接入。
- 7060容量、设备和特殊限制的现场核实。
- 数据保存期限、备份和恢复演练。
- 生产 HTTPS、密钥管理和数据库最小权限配置。
