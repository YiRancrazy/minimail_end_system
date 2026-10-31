package com.yirancrazy.minimall.order.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义结算后购物车清理任务的执行能力。
 *   业务边界：负责领取、校验和更新任务状态，通过订单远程网关调用购物车服务；不创建订单。
 *   使用场景：结算编排可立即触发，定时任务负责扫描重试。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
public interface CartCleanupService {

  /**
   * 尝试执行一条购物车清理任务。
   *
   * <p>已成功任务直接返回 true；待处理任务需先条件抢占，再解析 JSON、校验条目并调用购物车服务。可重试错误保持 PENDING，参数或 SKU 不匹配等错误直接失败。
   *
   * @param taskId 购物车清理任务 ID
   * @return 任务已成功或本次清理成功时返回 true。
   * @throws BizException 任务参数或远端错误会转换为任务状态；方法本身不向调度器抛出已捕获的运行时异常。
   */
  boolean tryCleanup(Long taskId);

  /**
   * 扫描并执行已到期的购物车清理任务。
   *
   * <p>单批最多 100 条，按 nextRetryAt 和 ID 排序；单条运行时异常不会阻断后续任务。
   *
   * @return 本轮成功清理的任务数量。
   */
  int scanPending();
}
