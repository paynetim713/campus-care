# CampusCare Database Setup for XAMPP MySQL

## 前置要求
1. 已安装 XAMPP 并启动 MySQL 服务
2. 端口默认为 3306

## 数据库配置

### 1. 创建数据库
打开 phpMyAdmin (http://localhost/phpmyadmin)，执行以下SQL:

```sql
CREATE DATABASE IF NOT EXISTS campus_care 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

### 2. 后端配置文件
确保 `application.yaml` 配置正确:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_care?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password:       # XAMPP默认密码为空
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update    # 会自动创建表结构
    show-sql: true
```

### 3. 启动后端
```bash
cd campus-care-backend
./mvnw spring-boot:run
# 或在Windows上
mvnw.cmd spring-boot:run
```

后端会在启动时自动创建所需的表。

### 4. 测试连接
后端启动后，会在 http://localhost:8080 运行。

### 5. Android前端连接
前端已配置连接 `http://10.0.2.2:8080` (Android模拟器访问宿主机)

如果使用真机测试，需要:
- 确保手机和电脑在同一WiFi网络
- 将 `ApiClient.java` 中的 BASE_URL 改为电脑的IP地址，例如:
```java
private static final String BASE_URL = "http://192.168.1.x:8080/";
```

## 角色说明
- **REQUESTER** - 学生/教师 (对应前端 "student")
- **TECHNICIAN** - 维修技术人员 (对应前端 "technician")  
- **ADMIN** - 管理员 (对应前端 "admin")

## 默认用户 (后端自动创建)
后端启动时会自动创建以下测试用户:

| 角色 | 邮箱 | 密码 |
|------|------|------|
| Admin | admin@uni.edu | admin12345 |
| Technician | tech@uni.edu | tech12345 |
| Student | john@uni.edu | user12345 |
| Teacher | sarah@uni.edu | user12345 |

## 启动顺序
1. 启动 XAMPP MySQL 服务
2. 在 phpMyAdmin 创建 `campus_care` 数据库
3. 启动后端: `cd campus-care-backend && mvnw.cmd spring-boot:run`
4. 后端会自动创建表结构和初始用户
5. 运行 Android 应用测试
