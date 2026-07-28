# minimall（薄荷商城后台）

Spring Cloud Alibaba 微服务，Java 17 + Spring Boot 3.2.x。

## 起步

```bash
./mvnw clean verify                                    # 全量编译 + 单测
docker compose -f infra/docker-compose.yml up -d      # 起 Nacos + MySQL + Redis
./mvnw -pl minimall-id-service spring-boot:run       # 起 id-service
```

## 模块

```
minimall-common         Result / 异常 / BasePO / @Manager（被所有服务依赖）
minimall-api            Feign 接口 + 跨服务 DTO
minimall-gateway        :8080 反向路由 + JWT 验签
minimall-id-service     :8211 雪花 ID
minimall-auth-service   :8201 登录 / 注册 / JWT 签发
minimall-user-service   :8202 用户档案 CRUD
minimall-merchant-service :8203 商家 / 店铺 CRUD
minimall-goods-service  :8204 商品 SPU / SKU
minimall-cart-service   :8205 购物车
```

## 验证

| 迭代 | 脚本 |
|------|------|
| Iter-0 | `./scripts/verify-iter0.sh` |
| Iter-1 | `./scripts/verify-iter1.sh` |

末尾输出 `PASS`。失败时容器/进程保留供排查（`docker ps` / `tail /tmp/auth.log`）。

## 快速验证（手动 curl）

```bash
# 注册
curl -X POST -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret"}' \
  http://127.0.0.1:8201/api/v1/auth/register

# 登录拿 token
TOK=$(curl -s -X POST -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret"}' \
  http://127.0.0.1:8201/api/v1/auth/login \
  | python -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")

# 拿当前用户
curl -H "Authorization: Bearer $TOK" \
  http://127.0.0.1:8201/api/v1/auth/me
```