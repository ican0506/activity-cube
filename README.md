# 活动魔方 activity-cube

活动魔方是面向高校校园活动组织场景的一站式轻工具平台，服务学生、活动负责人、系统管理员三类角色。它不是第二课堂替代系统，而是围绕校园活动组织过程提供创建、审核、报名、签到、通知、统计、反馈和成果沉淀的轻量工具。

## 技术栈

- 前端：Vue 3、Vite、Element Plus、Vue Router、Pinia、Axios、ECharts、qrcode、html5-qrcode、xlsx
- 后端：Java 17、Spring Boot 3.3、MyBatis Plus、MySQL、Maven、Flyway
- 数据库管理：Navicat
- 文件上传：本地外部目录 `uploads/`，浏览器访问路径 `/uploads/**`

## 功能模块

学生端：

- 活动大厅、活动详情、活动报名
- 扫码签到、线上签到、混合活动签到
- 我的活动、消息中心、个人中心
- 活动建议、问题反馈、满意度评价
- 活动成果、课外学时和积分记录

负责人端：

- 活动创建、活动管理、提交审核
- 二维码管理、报名名单、签到名单
- 人工补签、活动通知、名单导出
- 活动统计、反馈统计、奖励发放

管理员端：

- 活动管理、活动审核、用户管理
- 系统通知、操作日志、数据总览
- 创建负责人账号、管理所有活动数据

## 项目结构

```text
activity-cube/
├── backend/             Spring Boot 后端
├── frontend/            Vue 3 前端
├── docs/                项目文档和 SQL 补丁
├── sql/                 全新数据库初始化脚本
├── README.md            项目启动和交付说明
└── .gitignore           Git 忽略规则
```

## 数据库初始化

全新环境推荐执行：

```text
sql/activity_cube_final.sql
```

Navicat 操作建议：

1. 连接 MySQL。
2. 新建或选择 `activity_cube` 数据库。
3. 打开 `sql/activity_cube_final.sql`。
4. 一次性执行完整脚本。

已有数据库升级请不要执行初始化脚本，按需执行：

```text
docs/sql/*.sql
```

注意事项：

- `sql/activity_cube_final.sql` 面向全新数据库。
- 已有数据不要随便执行 DROP、TRUNCATE 或初始化脚本。
- 执行唯一约束补丁前，先执行 `docs/sql/duplicate_check.sql` 检查重复数据。
- 测试账号密码仍为 `123456`，数据库中存储的是 BCrypt 密文。

## 后端启动

使用 IDEA：

1. 用 IDEA 打开项目根目录或 `backend` 目录。
2. 确认 Project SDK 为 JDK 17。
3. 配置 MySQL 环境变量，或在 IDEA Run Configuration 中填写。
4. 运行 `ActivityCubeApplication`。

推荐环境变量：

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=activity_cube
DB_USER=root
DB_PASSWORD=你的数据库密码
UPLOAD_DIR=uploads
FRONTEND_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
```

命令行启动：

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

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

前端接口地址在 `frontend/.env.development` 中配置：

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

手机访问时，把 `localhost` 改成电脑局域网 IP，例如：

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

```text
管理员登录
→ 创建负责人账号
→ 负责人登录并创建活动
→ 负责人提交审核
→ 管理员审核通过
→ 学生进入活动大厅报名
→ 学生线上签到或扫码签到
→ 学生提交活动建议/评价
→ 负责人查看报名、签到、反馈和统计
```

## 手机访问和扫码说明

1. 手机和电脑需要连接同一局域网。
2. 前端用电脑 IP 访问，例如 `http://192.168.1.100:5173`。
3. 后端接口也要在 `VITE_API_BASE_URL` 中配置成电脑 IP。
4. 后端建议监听 `0.0.0.0` 或保持默认可被局域网访问。
5. 二维码使用当前浏览器的 `window.location.origin` 生成，不要用 `localhost` 页面生成给手机扫。
6. 手机浏览器在 HTTP 局域网环境下可能限制摄像头。
7. 摄像头不可用时，在 `/scan` 页面粘贴二维码链接或输入现场签到码。

## 常见问题

### Network Error

检查 `frontend/.env.development` 的 `VITE_API_BASE_URL` 是否指向正在运行的后端地址。手机访问时不能使用 `localhost`。

### 401 请先登录

接口需要登录 token。重新登录后再访问；如果切换过后端地址，清理浏览器 localStorage 后重试。

### 403 无权限

当前账号角色无权访问该接口。学生不能访问 `/admin/**`，负责人只能管理自己创建的活动。

### 8080 端口被占用

关闭占用 8080 的进程，或修改 `backend/src/main/resources/application.yml` 中的 `server.port`。

### 5173 端口被占用

Vite 会提示使用其他端口，也可以执行：

```bash
npm run dev -- --host 0.0.0.0 --port 5174
```

### 手机能打开前端但登录失败

通常是前端接口仍指向 `localhost`。把 `VITE_API_BASE_URL` 改成电脑局域网 IP 后重启前端。

### 二维码出现 localhost

生成二维码前，请用手机可访问的局域网地址打开前端，再进入二维码管理页生成。

### Maven 命令 mvn not found

可以用 IDEA Maven 面板运行，或安装 Maven 并配置 PATH。本机 IDEA 自带 Maven 也可以使用，但要配置正确的 `JAVA_HOME`。

### SQL 报字段已存在或表已存在

全新库执行 `sql/activity_cube_final.sql`。已有库执行 `docs/sql` 补丁；大部分补丁已做字段存在判断，仍报错时先确认是否已经执行过。

## 验证命令

前端：

```bash
cd frontend
node --test src\utils\*.test.mjs src\api\*.test.mjs
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
