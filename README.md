# 活动魔方

活动魔方是面向高校校园活动组织场景的一站式轻工具平台。当前版本优先完成比赛可演示 MVP：

活动负责人创建活动 -> 生成报名二维码 -> 学生报名 -> 生成签到二维码 -> 学生签到 -> 后台查看统计 -> 导出名单。

## 技术栈

- 前端：Vue 3、Vite、Element Plus、Vue Router、Pinia、Axios、ECharts、qrcode
- 后端：Java 17、Spring Boot 3、MyBatis Plus、MySQL、Maven
- 数据库：默认连接 Navicat 中 `fuck` 连接下的 `activity_cube` 数据库

## 目录

```text
activity-cube/
├── backend/
├── frontend/
├── docs/
├── docs/sql/activity_cube.sql
├── .env.example
└── README.md
```

## 初始化数据库

在 Navicat 的 `fuck` 连接中执行：

```sql
source docs/sql/activity_cube.sql;
```

如果 Navicat 不支持 `source`，打开 `docs/sql/activity_cube.sql` 后直接运行全部 SQL。

脚本会创建并使用 `activity_cube` 数据库，包含用户、活动、报名、签到、反馈五张核心表，并预置测试账号。

## 后端启动

在 IntelliJ IDEA 中打开项目根目录，加载 `backend/pom.xml` 为 Maven 项目。

配置环境变量：

```env
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=你的数据库密码
DB_NAME=activity_cube
```

运行：

```bash
cd backend
mvn spring-boot:run
```

或在 IDEA 中运行 `com.activitycube.ActivityCubeApplication`。

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

前端默认地址：

```text
http://localhost:5173
```

## 测试账号

| 账号 | 密码 | 角色 | 校区 |
|---|---|---|---|
| student001 | 123456 | 学生 | 龙子湖校区 |
| student002 | 123456 | 学生 | 文化路校区 |
| student003 | 123456 | 学生 | 许昌校区 |
| organizer001 | 123456 | 活动负责人 | 龙子湖校区 |
| admin | 123456 | 管理员 | 龙子湖校区 |

## 已完成 MVP

- 登录与简化 token
- 活动列表、详情、创建、编辑、删除
- 报名二维码、报名表单、报名名单
- 签到二维码、签到入口、签到名单、未签到名单
- 单活动统计和校区统计图表
- 随机抽签、随机分组
- 报名/签到 CSV 导出

## GitHub 注意事项

不要提交真实 `.env`、数据库密码、`node_modules/`、`dist/`、`target/`。

