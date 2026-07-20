# 活动魔方

活动魔方是面向河南农业大学校园活动组织场景的一站式轻工具平台，支持龙子湖校区、文化路校区、许昌校区。系统覆盖活动创建、活动审核、报名收集、二维码报名、线上/线下签到、人工补签、名单导出、随机抽签、随机分组、活动反馈、满意度统计、图片视频上传、消息中心和管理员用户管理。

## 技术栈

- 前端：Vue 3、Vite、Element Plus、Vue Router、Pinia、Axios、ECharts、qrcode、html5-qrcode、xlsx
- 后端：Java 17、Spring Boot 3.3、MyBatis Plus、Maven、Flyway
- 数据库：MySQL 8，默认数据库名 `activity_cube`
- 文件上传：本地外部目录 `uploads/`，浏览器访问路径 `/uploads/**`

## 数据库初始化

### 全新数据库

在 Navicat 中打开并执行：

```text
sql/activity_cube_final.sql
```

该文件会创建 `activity_cube` 数据库、完整表结构、唯一约束和测试账号。测试账号密码均为 `123456`，数据库中存储的是 BCrypt 密文。

### 已有数据库

不要删除现有表。建议按需执行 `docs/sql` 下的补丁：

```text
docs/sql/duplicate_check.sql
docs/sql/unique_constraints_patch.sql
docs/sql/password_bcrypt_patch.sql
docs/sql/activity_time_status_patch.sql
docs/sql/activity_mode_patch.sql
docs/sql/media_upload_patch.sql
docs/sql/manual_checkin_patch.sql
docs/sql/activity_review_operation_log_patch.sql
docs/sql/notice_center_patch.sql
docs/sql/activity_reward_patch.sql
```

执行唯一约束前必须先运行 `docs/sql/duplicate_check.sql`。如果查询结果不为空，先保留正确记录、删除重复记录，再执行 `docs/sql/unique_constraints_patch.sql`。

### Flyway

项目已加入 Flyway，迁移文件位于：

```text
backend/src/main/resources/db/migration
```

默认不自动执行，避免影响你本地已有 Navicat 数据库。新环境可以在后端启动前设置：

```env
FLYWAY_ENABLED=true
```

然后先在 MySQL 中创建空库 `activity_cube`，再启动 Spring Boot。

## 后端启动

IDEA 打开项目根目录，加载 `backend/pom.xml`。建议 JDK 使用 Java 17。

常用环境变量：

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=activity_cube
DB_USER=root
DB_PASSWORD=你的数据库密码
UPLOAD_DIR=uploads
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
npm run dev
```

前端接口地址由 `frontend/.env.development` 配置：

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

手机访问时，把 `localhost` 改成电脑局域网 IP，例如：

```env
VITE_API_BASE_URL=http://10.74.51.156:8080/api
```

修改 `.env.development` 后需要重启 `npm run dev`。

## 测试账号

| 角色 | 登录账号 | 密码 | 说明 |
|---|---|---|---|
| 学生 | 2321241389 | 123456 | 学号登录，张三 |
| 负责人 | T2024001 | 123456 | 工号登录，李老师 |
| 管理员 | admin | 123456 | 管理员账号 |

## 核心接口补充

- `POST /api/admin/activities/{id}/rewards/issue`：管理员或活动创建者给已签到学生发放活动奖励。
- `GET /api/admin/activities/{id}/rewards`：查看某活动奖励发放记录。
- `GET /api/student/rewards`：学生查看个人中心活动成果。

## 手机扫码访问

1. 电脑和手机连接同一个网络。
2. 前端使用局域网地址访问，例如 `http://10.74.51.156:5173`。
3. 后端接口使用局域网地址配置到 `VITE_API_BASE_URL`。
4. 二维码内容使用 `window.location.origin` 生成，不会写死 localhost。
5. 手机浏览器在局域网 HTTP 下可能无法启动摄像头，可在 `/scan` 页面粘贴二维码链接或输入现场签到码。

## 文件上传

- 文件保存到后端外部目录 `uploads/年/月/文件名`。
- 浏览器访问示例：`http://localhost:8080/uploads/2026/07/xxx.jpg`。
- 图片限制：jpg、jpeg、png、gif、webp，最大 5MB。
- 视频限制：mp4、webm、mov，最大 100MB。
- `uploads/` 已加入 `.gitignore`，不要提交到 GitHub。

## 常见问题

### Network Error

检查 `frontend/.env.development` 的 `VITE_API_BASE_URL` 是否是当前后端地址；手机访问时不要使用 `localhost`。

### 端口占用

后端默认 `8080`，前端默认 `5173`。如果端口被占用，先关闭占用进程，或修改 `backend/src/main/resources/application.yml` 的 `server.port`。

### 401 请先登录

说明接口需要 token。先登录，再访问受保护页面；如果刚改过接口地址，清理浏览器 localStorage 后重新登录。

### 二维码 localhost 问题

二维码根据当前浏览器地址生成。生成二维码前，请用手机可访问的局域网地址打开前端，再进入二维码管理页生成。

### 换网络后 IP 变化

电脑局域网 IP 变化后，需要同步修改 `frontend/.env.development`，重启前端，再重新生成二维码。

## 验证命令

后端：

```bash
cd backend
mvn test
```

前端：

```bash
cd frontend
npm run test
npm run build
```
