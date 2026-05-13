# ────────────────────────────────────────────────────────────────
# Root Dockerfile —— Railway 默认在仓库根找 Dockerfile,
# 所以这个文件负责进入 campus-care-backend 子目录构建后端。
# ────────────────────────────────────────────────────────────────

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY campus-care-backend/pom.xml .
RUN mvn -B -q dependency:go-offline

COPY campus-care-backend/src ./src
RUN mvn -B -q -DskipTests package

# ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

# uploads 目录:容器内运行时生成。生产建议挂 Railway Volume 到 /app/uploads
RUN mkdir -p /app/uploads

ENTRYPOINT ["sh","-c","java -jar app.jar --server.port=${PORT}"]
