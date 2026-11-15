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
