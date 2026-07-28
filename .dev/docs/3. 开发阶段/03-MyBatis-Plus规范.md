﻿﻿﻿﻿# MyBatis-Plus 规范

## 文档信息

| 字段 | 内容 |
|------|------|
| 文档名称 | MyBatis-Plus 规范 |
| 文档版本 | V1.0 |$
| 所属阶段 | 开发阶段 |
| 文档状态 | 已发布 |
| 创建人 | yirancrazy@gmail.com |
| 创建日期 | 2026-07-27 |
| 最后更新 | 2026-07-28 |[\$]

## 文档目的

> 规范 MyBatis-Plus 与原生 MyBatis 混合使用方式，统一 SQL 编写、映射、插件配置。

## 适用范围

- **适用对象**：Java 后端开发
- **适用场景**：Mapper 层开发、SQL 性能调优
- **不适用范围**：非 MyBatis 框架（如 JPA）

## 详细内容

### 1. 版本与依赖

| 组件 | 版本 |
|------|------|
| MyBatis | 3.5.x |
| MyBatis-Plus | 3.5.x |
| pagehelper（可选） | 不推荐，与 MP 分页插件冲突 |
| dynamic-datasource（多数据源） | 4.x |

详细选型见 [技术选型与决策记录（ADR）](../2.%20设计阶段/08-技术选型与决策记录（ADR）.md) ADR-002。

### 2. Mapper 规范

- 接口路径：`com.yirancrazy.minimall.mapper`
- 接口名：`XxxMapper`，继承 `BaseMapper<Xxx>`
- **禁止**接口名带 `I` 前缀（与 Service 区分）
- XML 路径：`src/main/resources/mapper/xxx/XxxMapper.xml`
- namespace 与接口全限定名一致

### 3. 实体类

```text
- 与表名对应：TradeOrder → trade_order（下划线命名）
- 必备字段：id / create_time / update_time / is_deleted
- 字段名与列名一致（启用 MP 下划线转驼峰）
- 逻辑删除：@TableLogic
- 字段填充：@TableField(fill = FieldFill.INSERT)
```

### 3.1 枚举持久化策略

枚举字段命名：`(code, alias, message)`，数据库存 `code(int)`：

```java
@Getter
@AllArgsConstructor
public enum OrderStatus implements IEnum<Integer> {
    UNPAID(10, "UNPAID", "待支付"),
    PAID(20, "PAID", "已支付"),
    CANCELLED(30, "CANCELLED", "已取消");

    private final Integer code;
    private final String alias;
    private final String message;

    @Override
    public Integer getValue() {
        return this.code;
    }
}
```

实体类使用：
```java
@TableField(value = "order_status")
@EnumValue // 标记code字段
private OrderStatus orderStatus;
```

优势：
- 查询高效、索引友好、存储省
- `code` 为整型，便于比较

详细选型见 [技术选型与决策记录（ADR）](../2.%20设计阶段/08-技术选型与决策记录（ADR）.md) ADR-045。

### 4. CRUD 使用

| 操作 | 方式 |
|------|------|
| 简单 CRUD | `BaseMapper` 自带方法 |
| 单表复杂查询 | `LambdaQueryWrapper` |
| 多表 / 复杂 SQL | XML 手写 |
| 批量插入 | `insertBatchSomeColumn`（需扩展） |
| 逻辑删除 | `@TableLogic` + `deleteById` 自动转 update |

**禁止**使用 `QueryWrapper`（非 lambda 方式，类型不安全）。

### 4.1 分层规则（Service vs Manager）

| 层 | 是否继承 `IService<T>` | 职责 |
|---|---|---|
| **Service** | **禁止**继承 | 薄业务层；只做流程性编排 + 事务边界；不直接调多个 Mapper |
| **Manager** | **必须**继承 | 跨 Mapper / 跨聚合的复杂业务编排；可直接复用 MP 通用 CRUD |

#### Service 层强制约束

- **禁止** Service 接口 `extends IService<T>` 或实现类 `extends ServiceImpl<M, T>`。
- Service 应当只依赖自己需要的 `Mapper` 或 `Manager`，由业务方法**显式编排** CRUD 调用。
- Service **禁止**直接编排多个 Mapper（属于 Manager 职责）。
- 如需复用 MP 的通用 CRUD 方法，直接在当前 Service 内**显式注入**对应 `BaseMapper<T>` 调用，不通过 `IService` 间接继承。

#### Manager 层强制约束

- **必须**让 Manager 继承 `IService<T>` + 实现 `extends ServiceImpl<M, T>`，直接复用 MP 通用 CRUD。
- Manager 负责：跨表 / 跨聚合的复杂业务编排、外部 RPC/MQ 调用、多 Mapper 事务编排。
- Manager **禁止**被 Controller 直接调用；只能由 Service 注入。
- 命名：`XxxManager`，存放于 `manager/` 包。

**反例**（禁止）：

```java
// 错误 1：Service 继承 IService，绕过 Manager 层
public interface OrderService extends IService<Order> {}
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {}

// 错误 2：Manager 未继承 IService，丢失通用 CRUD
@Manager
public class OrderManager {  // 缺少 extends IService<Order>
    private final OrderMapper orderMapper;
    // 还得手写 save / updateById / getById 等通用方法
}
```

**正例**：

```java
// Service：薄层，依赖 Mapper 和 Manager
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final OrderManager orderManager;

    @Override
    public Order getById(Long id) {
        return orderMapper.selectById(id);
    }

    @Override
    public void cancelOrder(Long id) {
        orderManager.cancel(id, "用户取消");
    }
}

// Manager：继承 IService + ServiceImpl，复用通用 CRUD，编排跨表业务
@Manager
public class OrderManager extends ServiceImpl<OrderMapper, Order> {
    private final UserMapper userMapper;
    private final InventoryMapper inventoryMapper;

    public void cancel(Long id, String reason) {
        Order order = getById(id);
        // 跨表编排
        userMapper.updateCancelStats(order.getUserId());
        inventoryMapper.restore(order);
        order.setStatus(OrderStatus.CANCELED);
        order.setCancelReason(reason);
        updateById(order);
    }
}
```

### 5. XML 规范

- resultMap 优先于 resultType
- 动态 SQL 使用 `<if>`、`<choose>`、`<where>`、`<set>`、`<foreach>`
- 拼接条件放 `<where>` 标签内，自动处理 AND/OR
- UPDATE 必须用 `<set>` 标签，自动去除末尾逗号
- **禁止** `${}` 拼接 SQL（防注入），必须 `#{}`
- 长 SQL 按逻辑分行，关键字大写
- 必须含注释说明业务含义

```xml
<select id="selectOrderList" resultMap="orderMap">
  SELECT id, order_no, user_id, status, create_time
  FROM trade_order
  <where>
    is_deleted = 0
    <if test="userId != null">AND user_id = #{userId}</if>
    <if test="status != null">AND status = #{status}</if>
    <if test="startTime != null">AND create_time >= #{startTime}</if>
  </where>
  ORDER BY create_time DESC
</select>
```

### 6. 分页

- **禁止**手动 `limit #{offset}, #{size}`
- 统一使用 MP `Page<T>` + `PageHelper` 风格方法
- 分页插件配置 `PaginationInnerInterceptor`
- 列表查询**禁止** `SELECT *`，明确字段
- 深分页（> 1000 页）使用 `last("limit ...")` 或游标分页

#### 分页查询方案

采用 MyBatis-Plus 原生 `Page<T>` + `IPage` 封装：

```java
// Manager层
public IPage<OrderPO> pageOrderList(Page<OrderPO> page, OrderQueryDTO query) {
    LambdaQueryWrapper<OrderPO> wrapper = Wrappers.lambdaQuery();
    // 构建查询条件
    return orderMapper.selectPage(page, wrapper);
}

// Controller层
public Result<IPage<OrderVO>> list(OrderQueryDTO query) {
    Page<OrderPO> page = new Page<>(query.getPageNum(), query.getPageSize());
    IPage<OrderPO> poPage = orderManager.pageOrderList(page, query);
    IPage<OrderVO> voPage = poPage.convert(this::toVO);
    return Result.success(voPage);
}
```

优势：
- 与 Manager 层 IService 无缝衔接
- VO 层转换简单（`page.convert`）
- 无需自定义分页 DTO

详细选型见 [技术选型与决策记录（ADR）](../2.%20设计阶段/08-技术选型与决策记录（ADR）.md) ADR-011。

### 7. 插件

| 插件 | 用途 | 配置类 |
|------|------|--------|
| 分页 | 物理分页 | `PaginationInnerInterceptor` |
| 乐观锁 | `@Version` | `OptimisticLockerInnerInterceptor` |
| 防止全表更新删除 | 强制带条件 | `PreventAttackInnerInterceptor` |
| 多租户 | 自动加租户条件 | `TenantLineInnerInterceptor` |

### 8. SQL 性能

| 规则 | 说明 |
|------|------|
| 单表 > 500 万行 | 建议分库分表 |
| 索引失效 | 避免对索引列使用函数 / 类型转换 / 表达式 |
| 联合索引 | 遵循最左前缀 |
| COUNT | 大表 COUNT 用近似值或单独维护计数表 |
| 慢 SQL | > 100ms 必须优化或加索引 |
| 联表 | 最多 3 张表，> 3 张考虑宽表 / 拆分 |
| ORDER BY | 必须命中索引，否则 filesort |

### 9. 数据库连接池

- Druid（已选型）：连接池 + SQL 监控 + 慢 SQL 日志 + 防火墙
- 必须配置：initial-size / min-idle / max-active / test-while-idle
- 密码加密存储（`ConfigFilter`）
- 开启慢 SQL 记录（`slowSqlMillis=1000`）
- 详细见 [部署运维文档模板](../5.%20部署阶段/02-部署运维文档模板.md) §6.2

### 10. 多数据源

- 使用 `dynamic-datasource` 注解切换：`@DS("slave")`
- 读写分离：主库 `@DS("master")`、从库 `@DS("slave")`
- 事务内强制走主库（避免主从延迟读不到）
- 多数据源配置文件按库拆分

### 11. 事务

- `@Transactional` 加在 Service 方法
- `rollbackFor = Exception.class`，避免 checked 异常不回滚
- 事务内**禁止**调 HTTP / RPC / MQ
- 嵌套事务传播：`REQUIRED`（默认）/ `REQUIRES_NEW`（独立事务）/ `NESTED`（保存点）
- 分布式事务参考 [系统架构设计文档](../2.%20设计阶段/01-系统架构设计文档.md) §7.2

### 12. 测试

- Mapper 测试使用 `@MybatisPlusTest`
- H2 内存库 + 真实 SQL
- `@Sql(scripts = "init.sql")` 初始化数据
- `@Sql(executionPhase = AFTER_TEST_METHOD, scripts = "cleanup.sql")` 清理
- 详细见 [测试策略与计划](../4.%20测试阶段/01-测试策略文档.md)

### 13. 反模式（禁止）

- ❌ 字符串拼接 SQL
- ❌ `SELECT *`
- ❌ Mapper 接口返回 `Map<String, Object>`
- ❌ 业务层写复杂 SQL（应放 XML）
- ❌ 跨服务 JOIN（远程 JOIN）
- ❌ 在事务内 sleep / await
- ❌ 使用物理外键约束（应用层保证）
- ❌ Service 继承 `IService<T>` / `ServiceImpl<M, T>`（详见 §4.1）

## 相关文档链接

- 上游：[Java 编码规范](01-Java编码规范.md)、[数据库设计文档](../2.%20设计阶段/03-数据库设计文档.md)
- 横向：[SpringBoot 使用规范](02-SpringBoot使用规范.md)
- 下游：[代码评审标准](07-代码评审标准.md)

## 变更记录

| 日期 | 版本 | 变更人/角色 | 变更说明 |
|------|------|-------------|----------|
| 2026-07-27 | V1.0 | yirancrazy@gmail.com | 初稿创建 |
| 2026-07-28 | V1.1 | yirancrazy@gmail.com | 新增分页查询方案（§6）和枚举持久化策略（§3.1） |
|      |      |             |          |
