# syntax=docker/dockerfile:1

# 多模块 Maven 工程，编译单个服务需连带父模块与依赖模块，
# 因此构建上下文必须为仓库根目录（由 compose 的 build.context 指定）。

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /opt/build
COPY . .
RUN chmod +x ./mvnw && ./mvnw -B -pl minimall-auth-service -am clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
COPY --from=build /opt/build/minimall-auth-service/target/minimall-auth-service.jar /app/app.jar
EXPOSE 8201
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]