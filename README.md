# Activity Cube 活动魔方

Activity Cube 是一个面向高校校园活动运营场景的 AI 驱动轻量平台，覆盖活动创建、审核、报名、签到、反馈、统计、通知、奖励沉淀和 AI 辅助运营。项目以河南农业大学多校区活动管理为背景，支持龙子湖校区、文化路校区、许昌校区，面向学生、活动负责人、系统管理员三类角色。

这个项目的定位不是传统后台管理系统，而是一个可以用于 AI 产品经理作品集展示的校园活动运营 SaaS 原型：用标准业务闭环证明产品能力，用 AI 文案、AI 复盘和 AI 推荐证明 AI 能力落地。

## 一句话介绍

让校园活动从“人工通知、表格报名、现场点名、事后无沉淀”，升级为“创建、报名、签到、反馈、数据复盘、AI 辅助运营”的完整闭环。

## 核心价值

- 对学生：快速发现活动、完成报名签到、查看消息和活动成果。
- 对负责人：高效创建活动、管理名单、处理反馈、查看数据和生成复盘。
- 对管理员：审核活动、管理用户、监控运营数据和维护平台秩序。
- 对作品集：展示完整产品链路、权限体系、数据闭环和 AI 应用设计能力。

## AI 能力

| AI 能力 | 使用位置 | 解决问题 | 设计原则 |
|---|---|---|---|
| AI 活动文案生成 | 活动创建 / 编辑页 | 帮助负责人快速生成活动标题、简介、亮点、报名须知和社群宣传文案 | AI 只生成草稿，必须由用户确认后写入表单 |
| AI 活动复盘 | 活动统计页 | 基于报名、签到、反馈数据生成活动复盘报告 | 只基于真实统计数据，不编造人数、评分和反馈 |
| AI 活动推荐 | 学生活动大厅 | 基于规则推荐活动，并由 AI 解释推荐理由 | 推荐活动来自真实活动数据，AI 只负责解释原因 |

AI 默认关闭，未配置 API Key 时不影响普通业务运行。

## 功能模块

学生端：

- 登录注册、统一账号登录
- 活动大厅、活动详情、已结束活动
- 活动报名、扫码签到、线上签到、混合活动签到
- 我的活动、消息中心、个人中心
- 活动建议、问题反馈、满意度评价
- 活动成果、课外学时和积分记录
- AI 个性化活动推荐

负责人端：

- Dashboard、活动管理、活动创建和编辑
- 活动二维码、报名名单、签到名单
- 人工补签、活动通知、名单导出
- 随机抽签、随机分组
- 反馈统计、活动统计、奖励发放
- AI 活动文案生成、AI 活动复盘

管理员端：

- 活动审核、活动管理、用户管理
- 创建负责人账号
- 系统通知、操作日志、数据总览
- 管理全部活动和平台数据

## 技术栈

前端：

- Vue 3
- Vite
- Element Plus
- Vue Router
- Pinia
- Axios
- ECharts
- qrcode
- xlsx

后端：

- Java 17
- Spring Boot 3.3
- MyBatis Plus
- MySQL
- Maven
- Flyway
- BCrypt
- OpenAI 兼容 HTTP 调用协议

工程化：

- 前后端分离
- 环境变量配置
- SQL 初始化脚本和补丁脚本
- 角色权限控制
- 文件上传目录隔离
- Git 忽略敏感文件和运行产物

## 项目结构

```text
activity-cube/
├── backend/                    Spring Boot 后端
├── frontend/                   Vue 3 前端
├── docs/                       项目文档、测试说明、SQL 补丁和展示材料
│   └── showcase/               作品集展示材料
├── sql/                        全新数据库初始化 SQL
├── uploads/                    本地上传目录，不提交 Git
├── README.md                   项目说明
└── .env.example                环境变量示例
```

## 数据库初始化

全新数据库推荐执行：

```text
sql/activity_cube_final.sql
```

Navicat 操作：

1. 连接 MySQL。
2. 新建数据库 `activity_cube`。
3. 打开 `sql/activity_cube_final.sql`。
4. 一次性执行完整脚本。

已有数据库升级不要执行初始化脚本，按需执行：

```text
docs/sql/*.sql
```

执行唯一约束前，先运行：

```text
docs/sql/duplicate_check.sql
```

## 后端启动

推荐用 IDEA：

1. 打开 `backend` 或项目根目录。
2. JDK 选择 17。
3. 配置 MySQL 和 AI 环境变量。
4. 运行 `ActivityCubeApplication`。

命令行：

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

环境变量示例：

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=activity_cube
DB_USER=root
DB_PASSWORD=你的数据库密码
UPLOAD_DIR=uploads
FRONTEND_ORIGINS=http://localhost:5173,http://127.0.0.1:5173

AI_ENABLED=false
AI_BASE_URL=https://api.deepseek.com
AI_API_KEY=
AI_MODEL=deepseek-chat
AI_TIMEOUT_SECONDS=30
```

说明：

- `AI_ENABLED=false` 时，AI 接口会返回友好提示，普通业务不受影响。
- `AI_BASE_URL`、`AI_MODEL`、`AI_API_KEY` 全部配置化，后续可切换 DeepSeek、通义千问或其他 OpenAI 兼容服务。
- 不要把真实 API Key 写入代码或提交到 GitHub。

## 前端启动

```bash
cd frontend
npm install
npm run dev -- --host 0.0.0.0
```

前端默认地址：

```text
http://localhost:5173
```

前端接口配置：

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

手机访问时需要把 `localhost` 改成电脑局域网 IP，例如：

```env
VITE_API_BASE_URL=http://192.168.1.100:8080/api
```

修改 `.env.development` 后需要重启前端服务。

## 测试账号

| 角色 | 登录方式 | 登录账号 | 密码 |
|---|---|---|---|
| 学生 | 学号登录 | 2321241389 | 123456 |
| 负责人 | 工号登录 | T2024001 | 123456 |
| 管理员 | 账号登录 | admin | 123456 |

## 推荐演示流程

1. 管理员登录，创建负责人账号并审核活动。
2. 负责人登录，创建活动并使用 AI 生成活动文案。
3. 管理员审核通过活动。
4. 学生进入活动大厅，查看 AI 推荐活动并报名。
5. 学生进行线上签到或扫码签到。
6. 学生提交活动建议、问题反馈或活动评价。
7. 负责人查看报名、签到、反馈和统计。
8. 活动结束后，负责人生成 AI 活动复盘。

## 作品集展示材料

展示材料放在：

```text
docs/showcase/
```

建议重点讲：

- 为什么做：校园活动组织割裂、数据不沉淀、反馈难复盘。
- 怎么做：三角色协同、活动生命周期、权限边界和数据闭环。
- AI 怎么落地：文案生成、活动复盘、活动推荐都嵌入真实业务节点。
- 产品亮点：不是聊天机器人，而是 AI 辅助活动运营。

## 手机访问和扫码说明

1. 手机和电脑连接同一局域网。
2. 前端用电脑 IP 访问，例如 `http://192.168.1.100:5173`。
3. 后端接口也要在 `VITE_API_BASE_URL` 中配置成电脑 IP。
4. 二维码使用当前浏览器的 `window.location.origin` 生成，不写死 localhost。
5. 手机浏览器在 HTTP 局域网下可能限制摄像头。
6. 摄像头不可用时，可在 `/scan` 页面粘贴二维码链接或输入现场签到码。

## 常见问题

### Network Error

检查 `frontend/.env.development` 中的 `VITE_API_BASE_URL` 是否指向正在运行的后端地址。手机访问时不能使用 `localhost`。

### 401 请先登录

接口需要登录 token。重新登录后再访问。如果切换过后端地址，清理浏览器 localStorage 后重试。

### 403 无权限

当前账号角色无权访问该接口。学生不能访问 `/admin/**`，负责人只能管理自己创建的活动。

### 二维码出现 localhost

生成二维码前，请用手机可访问的局域网地址打开前端，再进入二维码管理页生成。

### Maven 命令 mvn not found

可以使用 IDEA Maven 面板运行，或安装 Maven 并配置 PATH。

## 验证命令

前端：

```bash
cd frontend
npm run build
```

后端：

```bash
cd backend
mvn test
```

Git 检查：

```bash
git status
git diff --check
git ls-files node_modules dist target uploads .env .env.local
```

提交前不要包含 `node_modules/`、`dist/`、`target/`、`uploads/`、`.env`、`.env.local`、日志文件、真实数据库密码、本机局域网 IP、Token 或密钥。

## 近期工程化改动记录

> 说明：从 2026-08-13 起，后续每次完成代码修改、数据库变更、测试补充或重要工程化调整，都需要同步更新本节，避免 README 与实际项目状态脱节。

### 2026-08-12：认证、权限、并发与性能优化

本轮重点不是新增业务功能，而是把项目从「能跑的课程设计」继续往「可交付、可维护、可解释的工程项目」推进。

#### 1. JWT 认证改造

- 将原来的 `mock-token-{userId}` 替换为真实 JWT。
- JWT 包含用户 ID、角色、签发时间、过期时间和签发方等 Claims。
- 登录、注册返回结构保持不变，前端仍然按原方式保存 token。
- 后端认证流程改为：
  1. 读取 `Authorization: Bearer <token>`；
  2. 校验 JWT 签名、过期时间和签发方；
  3. 根据 token 中的 userId 查询数据库用户；
  4. 以数据库中的用户角色和状态作为最终权限依据；
  5. 写入 `UserContext`，请求结束后清理上下文。
- 新增 JWT 配置项：

```yaml
security:
  jwt:
    secret: ${JWT_SECRET:}
    expiration-seconds: ${JWT_EXPIRATION_SECONDS:7200}
    issuer: ${JWT_ISSUER:activity-cube}
```

本地运行时需要配置：

```env
JWT_SECRET=至少 32 字节的随机字符串
JWT_EXPIRATION_SECONDS=7200
JWT_ISSUER=activity-cube
```

注意：`JWT_SECRET` 不允许提交真实值到 Git。

#### 2. Spring Security 接入

- 引入 Spring Security 正式接管请求认证入口。
- 保留项目现有 `UserContext`，确保报名、签到、反馈、AI 等业务代码不需要大范围重构。
- 对登录、注册、静态上传资源和必要预检请求做放行。
- 普通业务接口继续通过统一认证链路获取当前用户。

#### 3. 方法级 RBAC 权限体系

- 在关键 Service / Controller 上补充角色权限控制。
- 明确三类角色边界：
  - `student`：浏览活动、报名、签到、反馈、查看个人数据；
  - `organizer`：管理自己创建的活动，查看名单、二维码、统计和反馈；
  - `admin`：管理全部活动、用户、审核、日志和系统通知。
- 管理员接口、负责人接口、学生个人接口不再只依赖前端隐藏入口，后端也做权限校验。

#### 4. 活动报名并发控制

- 在 `activity` 表增加 `registered_count` 业务计数器。
- 使用 MySQL 原子更新抢占名额：

```sql
UPDATE activity
SET registered_count = registered_count + 1
WHERE id = ?
  AND (max_participants IS NULL OR registered_count < max_participants)
```

- 报名事务边界调整为：
  1. 校验活动状态；
  2. 原子占用名额；
  3. 插入报名记录；
  4. 生成报名成功通知；
  5. 提交事务。
- 如果报名记录插入或通知生成失败，`registered_count` 会随事务回滚。
- 同一活动同一用户重复报名仍由应用层提前判断 + 数据库唯一约束兜底。
- 取消报名时同步减少 `registered_count`，并避免重复取消导致计数变负。
- 新增 Flyway 迁移：

```text
backend/src/main/resources/db/migration/V11__registration_concurrency.sql
```

#### 5. 数据库 Schema 一致性检查

- 检查 Flyway V1-V12 最终生成的核心表结构。
- 对比维护中的完整初始化 SQL 与实体字段。
- 补齐 `activity.checkin_mode` 迁移：

```text
backend/src/main/resources/db/migration/V12__activity_checkin_mode.sql
```

- 确认以下字段在当前工程中保持一致：
  - `activity_mode`
  - `checkin_mode`
  - `registered_count`
  - `activity_category`
  - 奖励相关字段
  - 报名、签到、活动时间字段
  - 活动状态字段

#### 6. 活动列表 N+1 查询优化

- 优化 `ActivityService.list()`，解决活动列表中按活动逐条查询报名、签到、反馈状态的问题。
- 原逻辑在学生端近似为：

```text
1 次活动列表查询 + 每个活动 5 次状态查询
```

- 新逻辑改为：
  1. 查询活动基础列表；
  2. 计算动态活动状态；
  3. 按状态完成过滤；
  4. 对最终活动 ID 列表批量查询报名数、签到数、学生报名状态、签到状态和反馈状态；
  5. 回填页面展示字段。
- 新增批量 Mapper 方法：
  - `RegistrationMapper.countByActivityIds`
  - `RegistrationMapper.findActivityIdsByUserAndActivityIds`
  - `CheckinMapper.countByActivityIds`
  - `CheckinMapper.findActivityIdsByUserAndActivityIds`
  - `FeedbackMapper.findActivityIdsByUserAndActivityIds`
- 新增通用计数 VO：

```text
backend/src/main/java/com/activitycube/vo/ActivityCountVO.java
```

- 优化后：
  - 学生端活动列表最多约 6 次 SQL；
  - 负责人 / 管理员活动列表最多约 3 次 SQL；
  - 空列表直接返回，不再执行批量 Mapper；
  - 循环内不再访问数据库，只从 `Map` / `Set` 中读取结果。

#### 7. 测试补充与验证结果

新增或完善的测试包括：

- JWT token 生成、解析、过期、篡改、issuer 校验；
- Spring Security 认证入口；
- 方法级 RBAC 权限；
- 报名并发控制单元测试；
- 真实 MySQL 报名并发集成测试；
- 活动列表批量查询单元测试；
- 活动列表批量 Mapper MySQL 集成测试。

已验证命令：

```bash
cd backend
mvn test
```

结果：

```text
Tests run: 156, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

真实 MySQL 集成测试命令：

```bash
cd backend
mvn -Pintegration-test -Dit.mysql.mode=local -Dit.mysql.password=root123 verify
```

结果：

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

说明：集成测试会创建独立测试库，例如 `activity_cube_concurrency_it` 和 `activity_cube_batch_mapper_it`，不会直接修改正式 `activity_cube` 数据库。

#### 8. 已知遗留问题

- 单元测试编译阶段仍可能提示既有测试中的 unchecked warning，目前不影响测试通过。

### 2026-08-13：反馈表 Schema Drift 修复

本轮只处理数据库结构一致性问题，不修改 N+1 优化代码、反馈业务逻辑、前端页面、报名并发算法、认证权限、AI 模块或分页逻辑。

#### 1. 修复原因

真实 MySQL 集成测试暴露出一个历史遗留问题：

- 通过 Flyway `V1` 到 `V12` 从零创建数据库时，`feedback` 表缺少 `feedback_type` 字段；
- 当前 `Feedback` 实体、`FeedbackService` 和维护中的完整初始化 SQL 已经按多类型反馈设计；
- 这导致 Flyway 最终 schema 与当前应用代码 / 完整初始化 SQL 不一致。

#### 2. 正式字段定义

`feedback.feedback_type` 当前正式定义为：

```sql
feedback_type VARCHAR(20) NOT NULL DEFAULT 'evaluation'
```

含义：

- `suggestion`：活动建议；
- `issue` / `problem`：问题反馈；
- `evaluation`：活动评价。

当前业务代码会把前端传入的 `issue` 兼容映射为 `problem`，同时 SQL 约束保留 `issue`，用于兼容历史数据和旧接口输入。

#### 3. 新增 Flyway 迁移

新增迁移文件：

```text
backend/src/main/resources/db/migration/V13__feedback_type.sql
```

该迁移完成：

- 补充 `feedback_type` 字段；
- 补充 `handle_status` 字段；
- 将 `score` 调整为可为空，仅活动评价需要评分；
- 将历史空 `content` 回填为空字符串，保证后续 `NOT NULL` 修改安全；
- 移除旧的 `uk_feedback_activity_user` 唯一约束，避免同一用户只能提交一种反馈；
- 增加 `idx_feedback_activity_user`、`idx_feedback_type`、`idx_feedback_user`；
- 增加反馈类型、处理状态、评分范围的检查约束。

历史数据兼容策略：

- 旧反馈记录默认回填为 `evaluation`；
- 旧处理状态默认回填为 `pending`；
- 旧空内容回填为空字符串；
- 迁移使用存在性判断，兼容已经手动执行过旧补丁 SQL 的本地数据库。

#### 4. 完整 SQL 同步

已同步维护中的完整初始化 SQL：

- `sql/activity_cube_final.sql`
- `sql/mysql-schema.sql`

其中 `feedback` 表字段、索引和约束已经与 `Feedback` 实体及当前业务逻辑保持一致。

#### 5. 集成测试修正

之前为了绕过缺失的 `feedback_type`，`ActivityListBatchMapperIntegrationIT` 使用 `JdbcTemplate` 手写插入反馈数据。

现在 Flyway schema 已经修复，测试恢复为真实路径：

- 使用 `FeedbackMapper.insert()`；
- 通过 `Feedback` 实体写入 `feedbackType`、`score`、`content`、`suggestion`、`handleStatus`、`anonymous` 等字段；
- `RegistrationConcurrencyIntegrationIT` 的 Flyway 版本校验更新到 `13`。

#### 6. 验证结果

单元测试：

```bash
cd backend
mvn test
```

结果：

```text
Tests run: 156, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

真实 MySQL 集成测试：

```bash
cd backend
mvn -Pintegration-test -Dit.mysql.mode=local -Dit.mysql.password=root123 verify
```

结果：

```text
Successfully applied 13 migrations
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

说明：集成测试会创建独立测试库，例如 `activity_cube_concurrency_it` 和 `activity_cube_batch_mapper_it`，不会直接修改正式 `activity_cube` 数据库。

### 2026-08-13：活动列表后端分页与查询条件下推

本轮只新增后端分页能力，不修改前端页面、不修改数据库结构、不修改 Flyway、不改报名并发算法和 AI 模块。

#### 1. 新增分页接口

新增接口：

```http
GET /api/activities/page
```

查询参数：

- `pageNum`：页码，默认 `1`；
- `pageSize`：每页数量，默认 `10`，最大 `50`；
- `keyword`：活动标题关键字；
- `campus`：活动校区；
- `status`：活动状态。

原有接口继续保留：

```http
GET /api/activities
```

前端当前仍可继续使用旧接口，后续前端分页迁移再单独进行。

#### 2. 分页参数处理

后端新增 `ActivityQueryRequest` 统一接收分页查询参数。

分页参数归一化规则：

- `pageNum <= 0` 或为空时，使用 `1`；
- `pageSize <= 0` 或为空时，使用 `10`；
- `pageSize > 50` 时，限制为 `50`。

分页返回复用项目已有结构：

```text
PageResult<Activity>
```

字段仍为：

- `records`
- `total`
- `page`
- `size`

没有新增第二套分页 VO。

#### 3. 查询条件下推

分页接口在数据库分页前完成查询条件过滤：

- `keyword`：继续保持当前语义，只按活动标题 `title LIKE` 搜索；
- `campus`：直接下推为 `campus = ?`；
- `status`：区分工作流状态和动态运行状态。

工作流状态直接使用数据库字段：

```text
DRAFT
PENDING_REVIEW
REJECTED
CANCELLED
PUBLISHED
```

动态运行状态会转换成 `PUBLISHED + 时间条件`：

```text
NOT_STARTED   -> PUBLISHED 且 now < register_start_time
REGISTERING   -> PUBLISHED 且 register_start_time <= now <= register_end_time
WAITING_START -> PUBLISHED 且 register_end_time < now < start_time
ONGOING       -> PUBLISHED 且 start_time <= now <= end_time
ENDED         -> PUBLISHED 且 now > end_time
```

这样可以保证：

- `records` 是过滤后的当前页数据；
- `total` 是同一过滤条件下的真实总数；
- 不会出现数据库先分页、Java 再过滤导致每页数量和总数错误的问题。

#### 4. N+1 优化复用

`ActivityService.list()` 和新增的 `ActivityService.page()` 共用同一套批量 enrichment 逻辑：

- 批量查询当前页活动报名数；
- 批量查询当前页活动签到数；
- 学生端批量查询当前页报名状态；
- 学生端批量查询当前页签到状态；
- 学生端批量查询当前页反馈状态。

分页后不会重新出现逐活动查询。

分页后的典型 SQL 数量：

- 学生端：约 `1 COUNT + 1 SELECT + 5 批量查询 = 7` 条；
- 负责人 / 管理员：约 `1 COUNT + 1 SELECT + 2 批量查询 = 4` 条。

#### 5. 排序规则

分页查询使用稳定排序：

```text
create_time DESC, id DESC
```

避免多条活动 `create_time` 相同导致跨页重复或遗漏。

#### 6. 测试与验证

新增或补充：

- `ActivityControllerTest`：验证 `/page` Controller 入口委托到 `ActivityService.page()`；
- `ActivityServiceTest`：验证分页参数归一化、空页不执行批量查询、当前页 enrichment；
- `ActivityListBatchMapperIntegrationIT`：使用真实 MySQL 验证动态状态分页、`total`、`records`、`pageSize=2`、`id DESC` 稳定排序和当前页学生态回填。

单元测试：

```bash
cd backend
mvn test
```

结果：

```text
Tests run: 159, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

真实 MySQL 集成测试：

```bash
cd backend
mvn -Pintegration-test -Dit.mysql.mode=local -Dit.mysql.password=root123 verify
```

结果：

```text
Successfully applied 13 migrations
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 2026-08-13：活动分页查询索引实测与 V14 迁移

本轮只处理 `activity` 列表分页查询的 MySQL 索引优化，不修改 Java 业务代码、前端页面、报名并发逻辑、认证权限或 AI 模块。

#### 1. 测试方式

- 在本地 MySQL 中创建临时性能测试表 `activity_perf`；
- 复制当前 `activity` 表结构；
- 构造约 `30000` 条活动数据，其中 `PUBLISHED` 约 `24000` 条；
- 使用 `EXPLAIN ANALYZE` 对比优化前后执行计划；
- 测试完成后删除 `activity_perf`，不污染正式业务数据。

#### 2. 优化前主要瓶颈

典型活动大厅查询：

```sql
SELECT id, title, status, campus, activity_mode, create_time
FROM activity_perf
WHERE status = 'PUBLISHED'
ORDER BY create_time DESC, id DESC
LIMIT 10;
```

优化前执行计划：

- `key = NULL`
- `type = ALL`
- `Extra = Using where; Using filesort`
- 实际扫描 `30000` 行，过滤出 `24000` 行；
- records 查询耗时约 `20.1ms`。

校区筛选场景虽然可以使用旧索引 `idx_activity_campus_status(campus,status)`，但仍需要 filesort：

- `campus + PUBLISHED` records 查询约 `45.2ms`；
- `campus + REGISTERING` records 查询约 `39.3ms`。

#### 3. 候选索引实测结果

候选索引一：

```sql
CREATE INDEX idx_activity_status_create
ON activity (status, create_time, id);
```

实测效果：

- 默认活动大厅 records 查询从约 `20.1ms` 降到约 `0.13ms`；
- 执行计划从 `ALL + filesort` 变为 `idx_activity_status_create + Backward index scan`；
- `activity_mode = online` records 查询从约 `72.4ms` 降到约 `0.16ms`；
- `REGISTERING / ONGOING / ENDED` records 查询从约 `75ms-89ms` 降到约 `0.26ms-0.44ms`。

候选索引二：

```sql
CREATE INDEX idx_activity_campus_status_create
ON activity (campus, status, create_time, id);
```

实测效果：

- `campus + PUBLISHED` records 查询从约 `45.2ms` 降到约 `0.12ms`；
- `campus + REGISTERING` records 查询从约 `39.3ms` 降到约 `0.28ms`；
- 该索引覆盖旧索引 `idx_activity_campus_status(campus,status)` 的左前缀能力，因此正式迁移中删除旧索引，避免长期维护重复索引。

#### 4. 本轮正式索引

新增 Flyway：

```text
backend/src/main/resources/db/migration/V14__activity_query_indexes.sql
```

正式索引调整：

```sql
DROP INDEX idx_activity_campus_status ON activity;

CREATE INDEX idx_activity_status_create
ON activity (`status`, `create_time`, `id`);

CREATE INDEX idx_activity_campus_status_create
ON activity (`campus`, `status`, `create_time`, `id`);
```

同步更新：

- `sql/activity_cube_final.sql`
- `sql/mysql-schema.sql`

干净初始化 SQL 中不再同时保留旧 `idx_activity_campus_status` 和新 `idx_activity_campus_status_create`。

#### 5. 已知边界

- 动态状态的 COUNT 查询仍然可能较重，因为 `REGISTERING / ONGOING / ENDED` 还需要结合时间字段过滤；
- `title LIKE '%keyword%'` 的包含搜索没有本质优化，普通 BTree 无法解决前导通配符搜索；
- 本轮不新增 `activity_mode` 独立索引，因为 records 查询已能通过 `status + create_time + id` 快速找到前 10 条。

#### 6. 验证结果

单元测试：

```bash
cd backend
mvn test
```

结果：

```text
Tests run: 159, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

真实 MySQL 集成测试：

```bash
cd backend
mvn -Pintegration-test -Dit.mysql.mode=local -Dit.mysql.password=root123 verify
```

结果：

```text
Successfully validated 14 migrations
Successfully applied 14 migrations
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 2026-08-13：Activity 表 Schema 一致性收尾

本轮只处理 `activity` 表结构一致性，不修改 Java 业务代码、前端页面、分页、报名并发、认证权限或 AI 模块。

- 从 `sql/activity_cube_final.sql` 和 `sql/mysql-schema.sql` 删除过时的 `idx_activity_start_time(start_time)` 定义；
- 保留已验证有真实查询路径的 `idx_activity_checkin_code(checkin_code)`；
- 新增 Flyway `V15__activity_constraints.sql`，补齐 Activity 表 CHECK constraints；
- 约束覆盖：
  - `activity_mode IN ('online', 'offline', 'hybrid')`
  - `checkin_mode IN ('online', 'qr', 'both')`
  - `campus IN ('全校区', '龙子湖校区', '文化路校区', '许昌校区', '线上')`
  - `status IN ('DRAFT', 'PENDING_REVIEW', 'REJECTED', 'PUBLISHED', 'CANCELLED')`
  - `max_participants IS NULL OR max_participants > 0`
  - 活动时间、报名时间、签到时间均要求结束时间大于开始时间

执行 V15 前已检查本地 `activity_cube.activity` 现有 22 条活动数据，没有发现违反上述 CHECK 的记录。
