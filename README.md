# CampusCare

校园报修小系统。学生发起工单，技术员接单处理，管理员看后台。Android 客户端 + Spring Boot 后端。

学校里有时报修流程挺乱的——表单、群消息、个人微信、当面口头，全在用。所以拿这个场景练手，做一套统一的报修工单流程。

## 包含两部分

```
CampusCare/             ← 仓库根目录
├── CampusCare/          Android 客户端（Java）
└── campus-care-backend/ Spring Boot 后端（Java 17）
```

放在一个仓库里，方便一起改。如果只关心其中一端可以只 clone 之后进对应子目录。

## 技术栈

**后端**：Spring Boot 3.5、Spring Data JPA、MySQL 8、Spring Mail（用于密码重置邮件）、springdoc OpenAPI、Lombok。

**Android**：原生 Java、AppCompat + Material、Retrofit + OkHttp + Gson、Glide（图片），minSdk 26 / targetSdk 34，用 ViewBinding。

数据库是 MySQL，本地用 XAMPP 起就行。生产部署用的 Railway（后端那个 Dockerfile 就是给 Railway 用的）。

## 功能

三个角色：

- **报修人（Requester）**：学生或老师，发起工单、上传图片、看进度、聊天、评价。
- **技术员（Technician）**：看待接单列表、接单、更新进度、和报修人沟通。
- **管理员（Admin）**：用户管理、所有工单总览、聊天列表。

工单状态流转大致是 `OPEN → ASSIGNED → IN_PROGRESS → COMPLETED → RATED`。

## 跑后端

需要 Java 17 + MySQL 8。

```bash
# 1. 建库
mysql -uroot -e "CREATE DATABASE IF NOT EXISTS campus_care CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 设环境变量（不设的话用默认值）
export MYSQLHOST=127.0.0.1
export MYSQLPORT=3306
export MYSQLDATABASE=campus_care
export MYSQLUSER=root
export MYSQLPASSWORD=

# 邮件相关（不填的话发邮件会 fail，但其他功能正常）
export APP_MAIL_HOST=smtp.example.com
export APP_MAIL_USERNAME=...
export APP_MAIL_PASSWORD=...
export APP_MAIL_FROM=...

# 3. 启动
cd campus-care-backend
./mvnw spring-boot:run     # Linux/Mac
mvnw.cmd spring-boot:run   # Windows
```

默认端口 8080。

- 健康检查：http://localhost:8080/api/health
- Swagger：http://localhost:8080/swagger-ui/index.html

数据库表是 JPA 自动建的（`ddl-auto=update`），后端启动后会自建初始账号：

| 角色 | 邮箱 | 密码 |
|---|---|---|
| Admin | admin@uni.edu | admin12345 |
| Technician | tech@uni.edu | tech12345 |
| Student | john@uni.edu | user12345 |

这些密码只用于本地演示.

## 跑 Android

需要 Android Studio。

打开 `CampusCare/` 子目录，等 Gradle Sync。后端地址默认指向 Railway 部署，本地调试需要改一下：

- 模拟器连本机后端 → 在 App 内的"服务器设置"页里改成 `http://10.0.2.2:8080/`
- 真机 → 改成你电脑的 LAN IP，比如 `http://192.168.1.5:8080/`，并确保手机和电脑在同一 WiFi

App 内置了一个服务器设置页（`ServerConfigHelper`）

## 上传目录

后端把图片存到运行目录下的 `./uploads/`。本地跑没问题，部署的时候要给容器挂一个持久卷上去，不然重启就丢了。



## 协议

MIT。
