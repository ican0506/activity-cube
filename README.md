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
