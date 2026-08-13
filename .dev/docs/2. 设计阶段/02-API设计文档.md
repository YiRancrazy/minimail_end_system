# API 设计文档

## 文档信息

| 字段 | 内容 |
|------|------|
| 文档名称 | API 设计文档 |
| 文档版本 | V1.0 |$
| 所属阶段 | 设计阶段 |
| 文档状态 | 已冻结 |
| 创建人 | yirancrazy@gmail.com |
| 创建日期 | 2026-07-27 |
| 最后更新 | 2026-08-05 |[\$]

## 文档目的

> 定义对外 HTTP API 的路径、参数、响应、错误码，作为前后端联调、测试、第三方对接的唯一契约。
> 读者：前端、测试、第三方对接方、架构师。

## 适用范围

- **适用对象**：前端、测试、第三方对接方
- **适用场景**：接口联调、Mock、对接、问题排查
- **适用版本**：V1.0
- **不适用范围**：内部模块接口（见 [接口设计文档](05-接口设计文档.md)）、数据库结构（见 [数据库设计文档](03-数据库设计文档.md)）

---

> 上游: [架构设计](01-系统架构设计文档.md) §4–§9 · 配套: [安全设计](04-安全设计文档.md), [数据库设计](03-数据库设计文档.md)
>
> 本文档定义薄荷商城面向三类角色的全部公开 REST 接口。内部服务间契约（OpenFeign）由各服务的 `api-commons/` 模块生成，约定见 §10。

---

## 目录

1. [设计原则](#1-设计原则)
2. [协议、版本与基础约定](#2-协议版本与基础约定)
3. [URL 设计](#3-url-设计)
4. [请求规范](#4-请求规范)
5. [响应规范](#5-响应规范)
6. [幂等与防重放](#6-幂等与防重放)
7. [分页、排序、过滤](#7-分页排序过滤)
8. [错误规范（Result\<T\> 统一返回）](#8-错误规范resultt-统一返回)
9. [安全相关 Header](#9-安全相关-header)
10. [OpenFeign 内部契约约定](#10-openfeign-内部契约约定)
11. [USER 端接口清单](#11-user-端接口清单)
12. [MERCHANT 端接口清单](#12-merchant-端接口清单)
13. [PLATFORM 端接口清单](#13-platform-端接口清单)
14. [附录 A：通用模型（DTO）](#14-附录-a通用模型dto)
15. [附录 B：典型时序示例](#15-附录-b典型时序示例)

---

## 1. 设计原则

| 序 | 原则 | 含义 |
|---|---|---|
| A0 | 资源命名复数 + 名词化 | `/api/v1/orders`，禁止动词化（`/createOrder`） |
| A1 | HTTP 语义化 | `GET` 安全幂等、`POST` 创建、`PUT` 全量替换、`PATCH` 部分更新、`DELETE` 删除 |
| A2 | 错误统一遵循 Result\<T\> | `application/json` |
| A3 | 所有业务接口必须鉴权 | 不留「公开接口」特殊路径（除注册/登录） |
| A4 | 写操作必须幂等 | `Idempotency-Key` 必填，TTL 24h |
| A5 | 时间戳一律 ISO 8601 UTC | `2025-12-19T03:14:07.123Z` |
| A6 | 金额一律以字符串返回，避免精度丢失 | `"payAmount": "1234.56"` |
| A7 | 分页基于 cursor 不可跳页 | cursor 即上一页最后一条的 `id` 或 `createTime` |
| A8 | ID 必须 BIGINT 雪花或业务号 | 禁止自增短 ID 暴露 |
| A9 | 路径清晰区分角色 + 模块 | `/api/v1/user/...` `/api/v1/merchant/...` `/api/v1/platform/...` |
| A10 | OpenAPI 3.0 规范 | 每个服务一个 OpenAPI YAML；网关聚合为统一文档 |

---

## 2. 协议、版本与基础约定

### 2.1 协议与传输

- 生产强制 HTTPS（TLS 1.2+，2025 年起 1.3）；
- HTTP 头 `Content-Type: application/json; charset=utf-8`；
- 请求/响应统一 UTF-8；
- 长连接（HTTP/1.1 Keep-Alive）由网关配置；客户端单 IP 连接数 ≤ 6。

### 2.2 版本策略

`/api/v<MAJOR>/...`；MAJOR 变更指**不兼容**变更（如字段移除、错误格式换型）。新增字段、参数化等兼容变更属于 MINOR，**必须在同一 MAJOR 下完成，禁止引入新的小版本号**。v1 至少维持 18 个月；新版本发布时 v1 进入 deprecate 期只修复不开发。

#### API 版本策略（ADR-041）

采用 URL 路径版本：`/api/v1/users`。

- 与 Controller V1 后缀对应（如 `UserControllerV1`）
- Gateway 路由清晰：`/api/v1/**` → user-service
- 后续升级：新增 `/api/v2/users`，v1 维护 6 个月过渡期

反模式：
- ❌ Header 版本：客户端需额外设置 Header
- ❌ 不版本化：后期升级困难

### 2.3 网关基础路径

```text
https://mall.example.com/api/v1/user/...
https://mall.example.com/api/v1/merchant/...
https://mall.example.com/api/v1/platform/...
```

三类路径在网关层已做强角色绑定：
- `USER` 角色的 token 调 `merchant/...` 或 `platform/...` → 403 `ROLE_FORBIDDEN`；
- `MERCHANT` 同理。

### 2.4 命名约定

| 项 | 风格 | 示例 |
|---|---|---|
| 路径段 | kebab-case | `/api/v1/merchant-orders` |
| 查询参数 | camelCase | `?merchantId=&createdAfter=` |
| 请求 / 响应字段 | camelCase | `payAmount`、`receiverName` |
| 错误类型 URL | kebab-case 域 | `/errors/order-not-found` |
| 业务单号 | `<ENTITY>-<TS>-<RANDOM>` | `OD20251219-A1B2C3D4` |

---

## 3. URL 设计

### 3.1 资源路径模板

```text
/{role}/{resource}                  # 集合
/{role}/{resource}/{id}             # 单个资源
/{role}/{resource}/{id}/{sub}       # 子资源
```

例：

```text
GET    /api/v1/user/orders                    # 用户订单列表
POST   /api/v1/user/orders                    # 创建订单
GET    /api/v1/user/orders/{orderNo}         # 订单详情
PATCH  /api/v1/user/orders/{orderNo}         # 局部更新（取消、改地址等）
POST   /api/v1/user/orders/{orderNo}/pay      # 动作：对订单发起支付
POST   /api/v1/user/orders/{orderNo}/cancel   # 动作：取消订单
```

> Google API Design Guide 风格的「冒号动作」（`:pay`, `:cancel`）用于**对资源的非 CRUD 动词操作**，避免创建动名词资源。

### 3.2 集合操作

| 操作 | 路径 | 备注 |
|---|---|---|
| 列表 | `GET /<resources>` | cursor 分页 |
| 搜索 | `GET /<resources>:search` 或 `/search/<resources>` | q + filter |
| 导出 | `POST /<resources>:export` | 异步，返回 task id |
| 批量 | `POST /<resources>:batch` | 单一 API 名 |
| 计数 | `GET /<resources}/_count` | Google 风格，下划线防与 `id` 冲突 |

### 3.3 嵌套 vs 独立资源

| 类型 | 嵌套 | 独立 |
|---|---|---|
| 用户地址 / 收货信息 | `/user/addresses/{id}` | 不嵌套在 user 下，避免过深 |
| 订单项 | `/user/orders/{orderNo}/items/{itemId}` | 短深度可接受 |
| 商品图片 | — | 独立 `/goods/images/{id}` |

最大嵌套深度 **3**，超过 3 折叠为 query：`?order_no=&...`。

### 3.4 action vs noun 决策表

| 场景 | 选择 | 例子 |
|---|---|---|
| 标准 CRUD | noun | `/goods/spus/{spuId}` |
| 资源上动作（不改变 URL 资源语义） | `/action` | `/orders/{orderNo}/pay` |
| 异步任务 | noun + `/tasks/{taskId}` | `/exports/{taskId}` |
| 跨实体编排 | 不暴露 RPC → 编排为单端调用 | 由 `order-service` 内聚合 |

---

## 4. 请求规范

### 4.1 公共请求头

| Header | 必填 | 备注 |
|---|---|---|
| `Authorization` | 必填 | `Bearer <access_token>` |
| `Content-Type` | POST/PUT/PATCH 必填 | `application/json` |
| `Accept` | 建议 | `application/json` |
| `Accept-Language` | 可选 | 当前仅 `zh-CN` |
| `X-Idempotency-Key` | 写必填 | UUID |
| `X-Client-Ts` | 写必填 | ISO 8601 UTC |
| `X-Client-Nonce` | 敏感写必填 | 6–16 char |
| `X-Trace-Id` | 可选 | 网关存在则沿用 |
| `User-Agent` | 必填 | 与 WAF 规则联动 |

### 4.2 请求体（JSON）

- 全部 UTF-8，缩进 0；
- 字段 camelCase（Java/Jackson 默认风格，与后端 POJO 字段名一致）；
- 时间字段：`"createdAt": "2025-12-19T03:14:07.123Z"`；
- 金额字段：字符串 `"payAmount": "1234.56"`，前后端都用 `BigDecimal`；
- 枚举：`"status": "PAID"`（字符串 SCREAMING_SNAKE），便于前后端共享；
- 集合：默认上限 100，超过需分页或异步导出；
- 客户端时间戳必须 ±5 分钟，否则 400 `CLOCK_SKEW`。

### 4.3 客户端示例

```http
POST /api/v1/user/orders HTTP/1.1
Host: mall.example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json; charset=utf-8
X-Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
X-Client-Ts: 2025-12-19T03:14:07.123Z
Accept: application/json

{
  "cartItemIds": [12345, 67890],
  "addressId": 10086,
  "remark": "请勿放门口",
  "useCouponCode": "NEW2025"
}
```

---

## 5. 响应规范

### 5.1 公共响应头

| Header | 含义 |
|---|---|
| `X-Trace-Id` | 全链路 trace id |
| `X-Rate-Limit-Limit` / `-Remaining` / `-Reset` | 限流余量 |
| `ETag` / `If-None-Match` | 资源版本（GET 缓存） |
| `Cache-Control` | 公共可缓存接口 |

### 5.2 响应体（JSON）

统一使用 `Result<T>` 包装结构，字段 camelCase：

```json
{
  "code": "00000",
  "message": null,
  "data": { /* 资源或资源集合 */ },
  "traceId": "5f3a1b2c4d5e6f7g8h9i0j"
}
```

- `code`：5 位字符串，`"00000"` 表示成功，`"1xxxx"` 业务错误，`"2xxxx"` 系统错误；
- `message`：面向用户的提示信息，成功时为 `null`；
- `data`：业务数据，成功时始终存在（即使为空数组 `[]` 或 `null`），失败时为 `null`；
- `traceId`：链路追踪 ID，便于排查问题；
- 单资源：`{ "code": "00000", "data": { ... } }`；
- 集合：`{ "code": "00000", "data": [...], "traceId": "..." }`；
- 长任务响应：`{ "code": "00000", "data": { "taskId": "...", "statusUrl": "..." } }`，前端轮询 `GET /tasks/{taskId}`。

### 5.3 HTTP 状态码规范

| 状态 | 含义 | 适用 |
|---|---|---|
| 200 | 成功 | GET / PATCH |
| 201 | 资源已创建 | POST（响应 Location 头） |
| 204 | 成功无内容 | DELETE |
| 304 | Not Modified | ETag 命中 |
| 400 | 请求语法/参数错误 | 客户端校验 |
| 401 | 未登录 / token 失效 | 鉴权 |
| 403 | 已登录但无权限 | RBAC 越权 |
| 404 | 资源不存在 | 资源或单据找不到 |
| 409 | 资源状态冲突 | 订单已支付后再取消 |
| 422 | 业务校验失败 | 库存不足、密码不合规 |
| 429 | 限流 | 限流命中 |
| 500 | 服务异常 | 服务端 bug |
| 502 / 503 / 504 | 上游异常 | 网关透传 |
| 507 | 配额超限 | 平台/商家功能额度 |

---

## 6. 幂等与防重放

### 6.1 客户端规则

- 写接口必须本地生成 UUID 作为 `X-Idempotency-Key`，保留 24h 后丢弃；
- 同一 key + 不同 body → 400 `IDEMPOTENCY_KEY_REUSED`；
- 同一 key + 相同 body → 返回首次结果；
- 敏感写接口（支付、退款、提现）额外携带 `X-Client-Ts` + `X-Client-Nonce`，TS 偏差 ≤ 5 分钟。

### 6.2 服务端规则

- `t_idempotency` 表 + 唯一索引，TTL 24h；
- 写接口事务开启前先尝试 `INSERT IGNORE`，若受影响行 = 0 → 查回原响应（缓存 or 反序列化第二次）；
- 跨服务调用（Feign）传递 `X-Idempotency-Key`。

### 6.3 接口幂等性（ADR-049）

支付、扣库存等核心接口采用 Token 机制防重复提交：

流程：
1. 提交前调用 `/api/v1/auth/idempotent-token` 获取 token
2. 提交时 Header 携带 `X-Idempotent-Token: {token}`
3. 后端校验 token 存在且有效后删除 token
4. 重复提交时 token 不存在，返回错误

实现：
```java
@Idempotent
@PostMapping("/orders")
public Result<OrderVO> createOrder(@RequestBody OrderDTO dto) {
    // 业务逻辑
}
```

Redis 存储 token，TTL = 业务超时时间 + 冗余时间。

---

## 7. 分页、排序、过滤

### 7.1 Cursor 分页

```http
GET /api/v1/user/orders?limit=20&cursor=eyJpZCI6MTIzNDU2fQ==
```

返回（统一 `Result<T>` 包装，游标信息放入 `data` 内）：

```json
{
  "code": "00000",
  "data": {
    "records": [ /* items */ ],
    "nextCursor": "eyJpZCI6MTAyMzQ1Nn0=",
    "hasMore": true,
    "limit": 20
  },
  "traceId": "5f3a1b2c4d5e6f7g8h9i0j"
}
```

- 游标字段为内部 cursor（ID 哈希或 `id:createTime`），不暴露业务信息；
- `limit` 上限 100，默认 20；
- 没有下一页时 `nextCursor: null`，`hasMore: false`。

### 7.2 排序

```http
GET /api/v1/user/orders?sort=-createTime,id
```

- `+` 升序 / `-` 降序；多字段逗号分隔；
- 不允许 `?order_by=` 这种容易被注入的命名；仅白名单字段。

### 7.3 过滤

```http
GET /api/v1/user/orders?status=PAID,SHIPPED&created_after=2025-12-01T00:00:00Z
```

- 字段必须白名单；
- 多值逗号分隔表示 `IN`；
- 时间用 ISO 8601；
- 字符串支持前缀匹配带 `*`（仅 ES 走通接口）。

### 7.4 字段过滤

```http
GET /api/v1/user/orders?fields=id,orderNo,payAmount,status
```

- 减少不必要字段返回；
- 默认全量；按需白名单字段。

### 7.5 计数

```http
GET /api/v1/user/orders/_count?status=PAID
```

返回 `{ "data": { "count": 12 } }`，专为大屏与红点设计，不走主列表接口。

---

## 8. 错误规范（Result\<T\> 统一返回）

### 8.1 响应 Content-Type

错误响应统一使用 `application/json`，结构遵循 `Result<T>` 统一返回体：

```json
{
  "code": "24001",
  "message": "订单 OD20251219-A1B2C3D4 不存在或不属于当前用户",
  "data": null,
  "traceId": "5f3a1b2c4d5e6f7g8h9i0j"
}
```

字段含义：

| 字段 | 含义 |
|---|---|
| `code` | 5 位字符串错误码（阿里规约），`"00000"` 成功、`"1xxxx"` 业务错误、`"2xxxx"` 系统错误 |
| `message` | 面向用户的错误提示信息 |
| `data` | 错误时为 `null` |
| `traceId` | 排查用 trace id |

### 8.2 错误码分类

code 字段为 5 位字符串（阿里规约），同时保留 alias 语义短码（用于日志和开发辨识）：

| code 范围 | alias 前缀 | 含义 | 例 |
|---|---|---|---|
| `14001`~`14999` | `AUTH_*` | 鉴权 | `"14001"` alias `TOKEN_EXPIRED` |
| `11001`~`11999` | `BIZ_*` | 业务校验 | `"11001"` alias `OUT_OF_STOCK` |
| `24001`~`24999` | `RES_*` | 资源 | `"24001"` alias `NOT_FOUND` |
| `14021`~`14029` | `RATE_*` | 限流 | `"14021"` alias `LIMITED` |
| `13001`~`13099` | `IDEM_*` | 幂等 | `"13001"` alias `KEY_REUSED` |
| `15001`~`15999` | `PAY_*` | 支付/退款 | `"15001"` alias `ALIPAY_DOWN` |
| `25001`~`25999` | `EXT_*` | 外部依赖 | `"25001"` alias `GATEWAY_TIMEOUT` |
| `20001`~`20999` | `SYS_*` | 系统 | `"20001"` alias `INTERNAL` |

详细编码表见附录 B 与 OpenAPI `components/errors.yaml`。

### 8.3 标准错误码示例

| code | alias | status | 含义 |
|---|---|---|---|
| `"14001"` | `TOKEN_EXPIRED` | 401 | access_token 过期 |
| `"14002"` | `TOKEN_REVOKED` | 401 | 已注销 |
| `"14003"` | `ROLE_FORBIDDEN` | 403 | 角色越权 |
| `"14004"` | `OBJECT_FORBIDDEN` | 403 | 水平越权 |
| `"11001"` | `OUT_OF_STOCK` | 422 | 库存不足 |
| `"11011"` | `PAY_TIMEOUT` | 409 | 订单已超时 |
| `"11031"` | `REFUND_DENIED` | 422 | 退款条件不满足 |
| `"13001"` | `KEY_REUSED` | 400 | 幂等键复用但 body 不一致 |
| `"14021"` | `LIMITED` | 429 | 触发限流 |
| `"24001"` | `NOT_FOUND` | 404 | 资源不存在 |
| `"25001"` | `ALIPAY_DOWN` | 503 | 支付宝异常 |
| `"20001"` | `INTERNAL` | 500 | 通用异常 |

---

## 9. 安全相关 Header

详见 [安全设计](04-安全设计文档.md)。这里列与 API 直接相关：

| Header | 用途 |
|---|---|
| `Authorization` | Bearer JWT |
| `X-Idempotency-Key` | 写幂等 |
| `X-Client-Ts` + `X-Client-Nonce` | 防重放 |
| `X-Internal-Token` | 内部 Feign 服务间 |
| `X-Merc-Id` | 商家多角色时强制声明（普通 token 用 `scope` 已含） |

---

## 10. OpenFeign 内部契约约定

业务服务间的内部调用走 OpenFeign，对外暴露在 K8s 集群内，**不进入网关**（避免被外网扫描）。约定：

- 内部接口路径：`/internal/<service>/...`；
- 内部接口必须同样使用 OpenAPI 注释；
- 入口使用 `X-Internal-Token` 鉴权（短期 JWT，scope 严格最小）；
- 内部调用不暴露给前端文档，仅运维可见；
- 示例：

```java
@FeignClient(name = "stock-service", path = "/internal/stock")
public interface StockInternalClient {
    @PostMapping("/reserve")
    StockReserveResponse reserve(@RequestBody StockReserveRequest req);
}
```

---

## 11. USER 端接口清单

> 所有路径前缀 `https://mall.example.com/api/v1/user`，鉴权 `USER-*` 权限码。每个接口都对应需求文档 §4.1.x 的功能 ID。

### 11.1 认证 (USER-AUTH-*)

| 方法 | 路径 | 权限码 | 说明 |
|---|---|---|---|
| POST | `/auth/register` | USER-AUTH-0002 | 注册；需要图形验证码 |
| POST | `/auth/login` | USER-AUTH-0001 | 登录；需图验/滑块；返回 access/refresh |
| POST | `/auth/logout` | USER-AUTH-0003 | 注销当前 token |
| PUT | `/auth/password` | USER-AUTH-0004 | 改密（旧密 + 新密） |
| POST | `/auth/password/reset/request` | USER-AUTH-0005 | 短信下行验证码 |
| POST | `/auth/password/reset/confirm` | USER-AUTH-0005 | 提交验证码 + 新密 |
| POST | `/auth/token/refresh` | USER-AUTH-0007 | 用 refresh 换 access |
| GET | `/auth/me` | — | 取当前 token 中的用户基本信息（前端导航用） |

请求体示例（登录）：

```json
{ "account": "13812345678", "password": "P@ssw0rd!", "captchaToken": "..." }
```

响应：

```json
{
  "data": {
    "access_token": "eyJhbGciOi...",
    "refresh_token": "rft_3X9...",
    "expires_in": 900,
    "token_type": "Bearer"
  }
}
```

### 11.2 商品 (USER-GOODS-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| GET | `/goods` | USER-GOODS-0001 / -0003 |
| GET | `/goods/{spuId}` | USER-GOODS-0002 |
| GET | `/goods/{spuId}/skus` | USER-GOODS-0002 |

列表响应（游标分页，`records[].minPrice` 为最低售价，单位元；无 SKU 时为 null）：

```json
{
  "code": "00000",
  "data": {
    "records": [
      { "spuId": 20001, "spuNo": "SPU20001", "title": "...", "subtitle": "...",
        "mainImageUrl": "...", "merchantId": 5, "minPrice": "199.00" }
    ],
    "nextCursor": "eyJpZCI6MTAyMzQ1Nn0=",
    "hasMore": false,
    "limit": 20
  },
  "traceId": "5f3a1b2c4d5e6f7g8h9i0j"
}
```

> 说明：`minPrice` 由列表接口按 `t_sku.price` 聚合，前端无需逐卡调详情取价。

详情响应（含 SPU/SKU 聚合）：

```json
{
  "code": "00000",
  "data": {
    "spuId": 20001,
    "title": "...",
    "merchant": { "merchantId": 5, "name": "..." },
    "skus": [
      { "skuId": 30001, "spec": { "颜色": "红", "尺码": "XL" }, "price": "199.00", "image": "..." }
    ],
    "detailHtml": "...",
    "isOnSale": true
  },
  "traceId": "5f3a1b2c4d5e6f7g8h9i0j"
}
```

### 11.3 购物车 (USER-CART-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| POST | `/cart/items` | USER-CART-0001 |
| DELETE | `/cart/items/{itemId}` | USER-CART-0002 |
| GET | `/cart/items` | USER-CART-0003 |
| PATCH | `/cart/items/{itemId}` | USER-CART-0004 / -0005 |
| PUT | `/cart/select-all` | USER-CART-0006 |
| DELETE | `/cart/items/clear` | USER-CART-0007 |
| POST | `/cart/items/{itemId}/favorite` | USER-CART-0008 |
| POST | `/orders/checkout` | USER-CART-0009 |

### 11.4 订单 (USER-ORDER-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| POST | `/orders` | USER-ORDER-0001 |
| GET | `/orders` | USER-ORDER-0004 |
| GET | `/orders/{orderNo}` | USER-ORDER-0005 |
| DELETE | `/orders/{orderNo}` | USER-ORDER-0003 |
| POST | `/orders/{orderNo}/cancel` | USER-ORDER-0002 |
| POST | `/orders/{orderNo}/pay` | USER-ORDER-0006 |
| POST | `/orders/{orderNo}/confirm` | USER-ORDER-0007 |
| POST | `/orders/{orderNo}/refunds` | USER-ORDER-0008 |
| GET | `/orders/{orderNo}/logistics` | USER-ORDER-0009 |

> 支付动作 `POST ...:pay` 返回 `{ pay_url, payment_no, expire_at }`，前端跳转支付宝。回调由支付宝异步到 pay-service，不经过前端。

### 11.5 支付 (USER-PAY-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| POST | `/orders/{orderNo}/payment` | USER-PAY-0001 / -0002 / -0004 |
| GET | `/orders/{orderNo}/payment/status` | USER-PAY-0003 |

### 11.6 消息 (USER-MSG-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| GET | `/notify/messages` | USER-MSG-0006 |
| GET | `/notify/messages/_count` | USER-MSG-0007 |
| POST | `/notify/messages/{id}/_read` | USER-MSG-0008 |
| POST | `/notify/messages/_read-all` | USER-MSG-0009 |
| DELETE | `/notify/messages/{id}` | USER-MSG-0010 |

推送通道：WebSocket `/api/v1/ws`，连接后由 `notify-service` 推送「新消息通知」事件，前端再主动调用 `/messages/_count` 或拉 `?is_read=false` 列表。

### 11.7 地址与个人资料（USER-\* 配套）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET / POST / DELETE | `/addresses` | 地址 CRUD |
| GET / PATCH | `/me/profile` | 个人资料 |
| GET / POST | `/favorites` | 收藏 |

---

## 12. MERCHANT 端接口清单

> 路径前缀 `https://mall.example.com/api/v1/merchant`，鉴权 `MERCHANT-*`。

### 12.1 商家认证 (MERCHANT-AUTH-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| POST | `/auth/register` | 同 USER-AUTH-0002 流程，差别在角色标记 |
| POST | `/auth/login` | MERCHANT-AUTH-0001 |
| POST | `/auth/logout` | MERCHANT-AUTH-0002 |
| PUT | `/auth/password` | MERCHANT-AUTH-0003 |

### 12.2 商家与资质

| 方法 | 路径 | 权限码 |
|---|---|---|
| GET / PATCH | `/merchants/me` | 商家基本信息 |
| GET / POST | `/merchants/me/qualifications` | 资质材料（图片 OSS 直传后回调） |
| GET | `/merchants/me/audit-log` | 历史审核记录 |

### 12.3 商品 (MERCHANT-GOODS-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| POST | `/goods/spus` | MERCHANT-GOODS-0001 |
| DELETE | `/goods/spus/{spuId}` | MERCHANT-GOODS-0002 |
| POST | `/goods/spus/{spuId}/on-shelf` | MERCHANT-GOODS-0003 |
| POST | `/goods/spus/{spuId}/off-shelf` | MERCHANT-GOODS-0004 |
| PATCH | `/goods/spus/{spuId}` | MERCHANT-GOODS-0005 |
| GET | `/goods/spus` | MERCHANT-GOODS-0006 |
| GET | `/goods/spus/{spuId}/audit-records` | — 复用 PLATFORM-GOODS-0004 但限定自家 |

### 12.4 订单 (MERCHANT-ORDER-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| GET | `/orders` | MERCHANT-ORDER-0001 |
| GET | `/orders/{orderNo}` | MERCHANT-ORDER-0002 |
| POST | `/orders/{orderNo}/ship` | MERCHANT-ORDER-0003（传运单号 + 物流公司） |
| POST | `/orders/{orderNo}/cancel` | MERCHANT-ORDER-0004 |
| POST | `/refunds/{refundNo}/approve` / `/reject` | MERCHANT-ORDER-0005 |
| POST | `/orders/export` | MERCHANT-ORDER-0006（异步导出） |
| GET | `/orders/_count?status=...` | MERCHANT-ORDER-0007（待办红点） |

### 12.5 库存 (MERCHANT-STOCK-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| POST | `/stock/skus/{skuId}/initialize` | MERCHANT-STOCK-0001 |
| POST | `/stock/skus/{skuId}/adjust` | MERCHANT-STOCK-0002 |
| PUT | `/stock/skus/{skuId}/threshold` | MERCHANT-STOCK-0003 |
| GET | `/stock/skus/{skuId}` | MERCHANT-STOCK-0004 |
| GET | `/stock/journals` | MERCHANT-STOCK-0005 |
| POST | `/stock/journals/export` | MERCHANT-STOCK-0006 |

### 12.6 支付 (MERCHANT-PAY-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| GET | `/pay/journals` | MERCHANT-PAY-0001 收款流水 |
| POST | `/refunds/{refundNo}/execute` | MERCHANT-PAY-0002 |
| POST | `/withdrawals` | MERCHANT-PAY-0003 |
| GET | `/pay/summary` | MERCHANT-PAY-0004 |

### 12.7 消息 (MERCHANT-MSG-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| GET | `/notify/messages` | MERCHANT-MSG-0005 |
| GET | `/notify/messages/_count` | 复用 |
| POST | `/notify/messages/{id}/_read` | MERCHANT-MSG-0006 |
| WS | `/api/v1/ws?role=MERCHANT` | 推送 |

---

## 13. PLATFORM 端接口清单

> 路径前缀 `https://mall.example.com/api/v1/platform`，鉴权 `PLATFORM-*`，需要平台管理员 token。

### 13.1 平台认证与运营

| 方法 | 路径 | 权限码 |
|---|---|---|
| POST | `/auth/login` | PLATFORM-AUTH-0001 |
| POST | `/auth/logout` | PLATFORM-AUTH-0002 |
| GET / POST | `/roles` | PLATFORM-AUTH-0003 角色 CRUD |
| POST | `/roles/{roleId}/permissions` | 权限分配 |
| GET / POST | `/admins` | 平台管理员 |
| POST | `/merchants/{merchantId}/qualifications/audit` | PLATFORM-AUTH-0004（商家资质审核：body `decision ∈ {APPROVE, REJECT}`，对应 `t_merch_audit_log`） |

### 13.2 商品审核 (PLATFORM-GOODS-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| GET | `/goods/spus/pending` | PLATFORM-GOODS-0001 |
| POST | `/goods/spus/{spuId}/approve` | PLATFORM-GOODS-0002 |
| POST | `/goods/spus/{spuId}/reject` | PLATFORM-GOODS-0003（必传 reason） |
| GET | `/goods/spus/{spuId}/audit-records` | PLATFORM-GOODS-0004 |

### 13.3 订单 / 退款仲裁 (PLATFORM-ORDER-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| GET | `/orders` | PLATFORM-ORDER-0001 |
| GET | `/orders/{orderNo}` | PLATFORM-ORDER-0002 |
| POST | `/disputes/{caseNo}/arbitrate` | PLATFORM-ORDER-0003 |
| POST | `/orders/export` | PLATFORM-ORDER-0004 |
| GET | `/orders/summary` | PLATFORM-ORDER-0005（财务对账） |
| POST | `/orders/{orderNo}/force-close` | PLATFORM-ORDER-0006 |

### 13.4 支付 / 对账 (PLATFORM-PAY-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| GET | `/pay/transactions` | PLATFORM-PAY-0001 |
| POST | `/pay/reconciliation/export` | PLATFORM-PAY-0002 |
| GET | `/pay/statements` | PLATFORM-PAY-0003 |
| POST | `/withdrawals/{withdrawNo}/approve` | PLATFORM-PAY-0004 |
| POST | `/orders/{orderNo}/payment/freeze` | PLATFORM-PAY-0005 |

### 13.5 库存治理 (PLATFORM-STOCK-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| GET | `/stock/summary` | PLATFORM-STOCK-0001 |
| POST | `/stock/transfers` | PLATFORM-STOCK-0002 |
| POST | `/stock/take-stock/start` | PLATFORM-STOCK-0003 |
| GET | `/stock/anomalies` | PLATFORM-STOCK-0004 |
| POST | `/stock/warnings/broadcast` | PLATFORM-STOCK-0005 |

### 13.6 消息 / 公告 (PLATFORM-MSG-*)

| 方法 | 路径 | 权限码 |
|---|---|---|
| GET | `/system/alerts` | PLATFORM-MSG-0001 |
| GET | `/complaints` | PLATFORM-MSG-0002 |
| POST | `/announcements` | 公告发布 |
| GET | `/notify/messages` | PLATFORM-MSG-0004 |

---

## 14. 附录 A：通用模型（DTO）

### 14.1 通用响应外壳

```typescript
// 通用 TypeScript 类型
type ApiResponse<T> = {
  data: T;
  meta?: { [k: string]: any };
};
type ApiResponseList<T> = {
  data: T[];
  meta: {
    nextCursor: string | null;
    hasMore: boolean;
    limit: number;
  };
};
```

### 14.2 Money / Amount

- 传输：字符串 `"123.45"`；
- 内部：Java `BigDecimal` scale=2, `RoundingMode.HALF_EVEN`；
- 不允许 FLOAT。

### 14.3 时间

- 字段类型：`date-time` (ISO 8601 UTC)；
- 不允许 epoch 时间戳。

### 14.4 ID

- 用户 / 商家 / SKU / SPU / 订单 主键 雪花 BIGINT；
- 业务流水号（`order_no`、`payment_no`）字符串。

### 14.5 分页请求参数

```typescript
type PageQuery = {
  limit?: number;          // 默认 20，最大 100
  cursor?: string;        // 下一页游标
  sort?: string;          // 例: "-created_at,id"
  fields?: string;        // 例: "id,order_no,status"
};
```

### 14.6 错误响应（OpenAPI components）

```yaml
components:
  schemas:
    ProblemDetails:
      type: object
      required: [type, title, status, detail]
      properties:
        type: { type: string, format: uri }
        title: { type: string }
        status: { type: integer }
        detail: { type: string }
        instance: { type: string }
        code: { type: string }
        errors:
          type: array
          items:
            type: object
            properties:
              field: { type: string }
              message: { type: string }
        trace_id: { type: string }
        docs: { type: string, format: uri }
```

### 14.7 速率限制响应头

| Header | 例 |
|---|---|
| `X-Rate-Limit-Limit` | `100` |
| `X-Rate-Limit-Remaining` | `42` |
| `X-Rate-Limit-Reset` | `1734567890`（Unix 秒）|
| `Retry-After` | `12`（秒，429 响应）|

---

## 15. 附录 B：典型时序示例

### 15.1 用户下单

```mermaid
sequenceDiagram
    autonumber
    participant App as Web (前端)
    participant GW as API Gateway
    participant ORD as order-service
    participant CRT as cart-service
    participant STK as stock-service
    participant PAY as pay-service
    participant Ali as 支付宝

    App->>GW: POST /api/v1/user/orders<br/>X-Idempotency-Key=...
    GW->>ORD: 透传+JWT
    ORD->>CRT: 取勾选购物车
    ORD->>STK: /internal/stock/reserve
    ORD->>ORD: 开启 Seata AT (order_db + stock_db)
    ORD-->>GW: 201 Created {order_no, pay_url}
    App->>Ali: 跳转 pay_url
    Ali-->>PAY: 异步回调 (notify)
    PAY->>ORD: 内部更新订单为 PAID (MQ)
```

### 15.2 商品列表

```mermaid
sequenceDiagram
    participant App as Web
    participant GW as API Gateway
    participant GDS as goods-service
    participant ES as Elasticsearch
    App->>GW: GET /api/v1/user/goods?q=...&limit=20&cursor=...
    GW->>GDS: 转
    GDS->>ES: 搜索 (n+1 缓存)
    GDS-->>App: { data: [...], meta: {nextCursor} }
```

### 15.3 退款申请

```mermaid
sequenceDiagram
    participant U as 用户
    participant ORD as order-service
    participant MQ as RocketMQ
    participant MER as merchant-service
    U->>ORD: POST /orders/{orderNo}/refunds
    ORD->>MQ: REFUND_REQUESTED
    MQ-->>MER: 商家待办
    ORD-->>U: 201 { refund_no }
    MER->>ORD: POST /refunds/{refundNo}:approve
    ORD->>PAY: /internal/pay/refund
    PAY-->>Ali: 退款请求
    Ali-->>PAY: 异步结果
    PAY->>MQ: REFUND_RESULT
    MQ-->>ORD: 更新状态
    MQ-->>U: 站内信 + WS
```

---

> 末尾说明：本 API 设计为初版。**任何接口变更必须先改本文件再出实现**，由网关与各服务 OpenAPI YAML 同步；变更记录入 `docs/adr/` 目录。

---

## 16. 文件上传接口规范（分片中转）

> 前后端统一定义。文件不再由前端直传 MinIO，改为前端分片上传至后端服务，后端合并后写入 MinIO，返回 objectKey。

### 16.1 接口路径

USER 端前缀 `/api/v1/user/upload`，MERCHANT 端前缀 `/api/v1/merchant/upload`，两端口径与数据结构完全一致。

### 16.2 上传分片（POST {prefix}/chunk）

请求：`multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 单个分片（前端按 2MB 切片） |
| uploadId | String | 是 | 上传任务 ID（前端每次选文件生成 UUID） |
| chunkIndex | int | 是 | 分片序号，0 起 |
| totalChunks | int | 是 | 总分片数 |

响应：`Result<ChunkResultVO>`

```json
{ "code": "00000", "message": "ok", "data": { "done": false, "received": 2, "objectKey": null } }
```

- `done=false`：仅保存分片，`objectKey=null`，`received` 为已收分片数。
- `done=true`：所有分片已到齐，后端已合并上传 MinIO，`objectKey` 为最终文件 key。

### 16.3 查询已收分片（GET {prefix}/status）

请求参数：`uploadId`、`totalChunks`

响应：`Result<ChunkStatusVO>`

```json
{ "code": "00000", "message": "ok", "data": { "uploadId": "t", "receivedChunks": [0, 2], "done": false } }
```

前端据 `receivedChunks` 跳过已传分片，实现断点续传。

### 16.4 错误码（上传专用段 20030-20033）

| code | 含义 |
|------|------|
| 20030 | 分片参数非法（序号/总数非法） |
| 20031 | 分片保存失败 |
| 20032 | 分片合并失败 |
| 20033 | 上传任务不存在 |

### 16.5 分片存储与合并

- 分片落本地临时目录 `{java.io.tmpdir}/mm-chunk/{uploadId}/chunk-{i}`。
- 收满后在服务端按序号合并为单个临时文件，再整体上传 MinIO（objectKey=uploadId），随后清理临时目录。
- 网关放行 `/api/v1/{user,merchant}/upload/**`，无需鉴权；MinIO 上传组件仅在 `minimall.minio.endpoint` 配置存在时装配。

---

## 相关文档链接

- 上游：[系统架构设计文档](01-系统架构设计文档.md)、[需求规格说明书（SRS）](../1.%20需求阶段/02-需求规格说明书（SRS）.md)
- 横向：[接口设计文档](05-接口设计文档.md)、[数据库设计文档](03-数据库设计文档.md)、[安全设计文档](04-安全设计文档.md)
- 下游：[3. 开发阶段](../3.%20开发阶段/)、[4. 测试阶段](../4.%20测试阶段/)、[5. 部署阶段](../5.%20部署阶段/)
- 参考：阿里规约、OpenAPI 3.0、RESTful 规范

## 变更记录

| 日期 | 版本 | 变更人/角色 | 变更说明 |
|------|------|-------------|----------|
| 2026-07-27 | V1.0 | yirancrazy@gmail.com | 初稿创建，合并旧 API 设计文档内容 |
| 2026-07-28 | V1.1 | yirancrazy@gmail.com | 新增API版本策略（ADR-041）和接口幂等性方案（ADR-049） |
| 2026-08-13 | V1.2 | yirancrazy@gmail.com | 新增第 16 章文件上传分片中转规范；移除前端直传 MinIO 的预签名方案 |
