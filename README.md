# minimall（薄荷商城后台）

Spring Cloud Alibaba 微服务，Java 17 + Spring Boot 3.2.x。

## 起步

```bash
./mvnw clean verify                 # 全量编译 + 单测
docker compose -f infra/docker-compose.yml up -d   # 起 Nacos + MySQL + Redis
./mvnw -pl minimall-id-service spring-boot:run    # 起 id-service
```

## 模块

```
minimall-common   Result / 异常 / BasePO / @Manager（被所有服务依赖）
minimall-api      Feign 接口 + 跨服务 DTO（被业务服务依赖；common 不依赖它）
minimall-gateway  :8080 反向路由
minimall-id-service :8211 雪花 ID
minimall-auth-service :8201 P0 脚手架（Iter-1 之前不会扩）
```