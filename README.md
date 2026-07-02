# AI 提示词模板平台课程设计

项目已经拆分为前后端两个子项目：

```text
数据库原理课设
├── backend   Spring Boot + MyBatis-Plus 后端 API
└── frontend  原生 HTML/CSS/JS 前端页面
```

## 后端启动

1. 先在 MySQL 中创建数据库：

```sql
CREATE DATABASE ai_prompt_template_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 打开 `backend/src/main/resources/application.properties`，填写 MySQL 账号密码。

3. 启动后端：

```bash
cd backend
mvn spring-boot:run
```

项目默认使用 MySQL，启动时会自动执行：

- `sql/schema-mysql.sql`
- `sql/data-demo.sql`

启动后后端 API 地址为：

```text
http://localhost:8080/api
```

主要接口：

- `GET /api/templates/search`
- `GET /api/templates/{id}`
- `POST /api/orders`
- `POST /api/templates/{id}/use`
- `POST /api/reviews`
- `GET /api/profile`
- `GET /api/stats/platform`

## 前端启动

直接用浏览器打开：

```text
frontend/index.html
```

前端默认请求 `http://localhost:8080/api`。如果后端端口变化，可以通过 URL 参数覆盖，例如：

```text
frontend/index.html?apiBase=http://localhost:8081/api
```

## 本次优化点

- 后端改为 API 服务，旧 Thymeleaf 页面已移到独立前端思路中。
- `PortalService` 保留统一门面，Controller 只依赖统一入口。
- 购买流程增加重复购买判断，已购买模板再次购买会复用已有订单。
- 使用付费模板前会校验是否已购买。
- 模板使用版本优先读取 `prompt_template.current_version_id`。
- 取消收藏接口真正删除收藏记录并更新计数。
- 注册密码改为哈希存储，登录会校验密码。
- 新前端支持搜索、详情、购买、收藏、使用、评价、个人中心和统计看板。

数据库表结构和初始化数据未改动。
