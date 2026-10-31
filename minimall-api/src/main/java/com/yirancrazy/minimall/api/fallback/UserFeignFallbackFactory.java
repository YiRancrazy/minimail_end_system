package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVo;
import com.yirancrazy.minimall.api.dto.user.UserProvisionDto;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDto;
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在用户服务不可用时返回匿名快照、空管理结果或明确失败.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {
  /**
   * 为用户远程调用异常创建降级客户端.
   *
   * @param cause 用户远程调用异常
   * @return 用户降级客户端
   */
  @Override
  public UserFeignClient create(Throwable cause) {
    log.warn("user-service 不可达: {}", cause.getMessage());
    return new UserFeignClient() {
      /**
       * 用户服务不可用时拒绝创建业务档案.
       *
       * @param dto 用户档案参数
       * @return 系统错误响应
       */
      @Override
      public Result<Void> provision(UserProvisionDto dto) {
        return Result.fail(CommonCode.SYS_ERROR, "user-service 不可达");
      }

      /**
       * 用户服务不可用时返回匿名用户快照.
       *
       * @param id 用户 ID
       * @return 用户 ID 为 -1 的匿名快照
       */
      @Override
      public Result<UserSnapshotDto> snapshot(Long id) {
        return Result.success(new UserSnapshotDto(-1L, "unknown", "ANONYMOUS"));
      }

      /**
       * 用户服务不可用时拒绝新增收藏.
       *
       * @param userId 用户 ID
       * @param skuId SKU ID
       * @return 系统错误响应
       */
      @Override
      public Result<Void> addFavorite(Long userId, Long skuId) {
        return Result.fail(CommonCode.SYS_ERROR, "user-service 不可达");
      }

      /**
       * 用户服务不可用时返回保持请求数量上限的空分页.
       *
       * @param query 游标分页条件，可为空
       * @return 空用户分页结果
       */
      @Override
      public Result<CursorPageVo<UserManageVo>> pageManage(InternalPageQuery query) {
        int limit = query == null || query.getLimit() == null ? 20 : query.getLimit();
        return Result.success(CursorPageVo.empty(limit));
      }

      /**
       * 用户服务不可用时返回空管理详情.
       *
       * @param userId 用户 ID
       * @return 数据为空的成功响应
       */
      @Override
      public Result<UserManageVo> detail(Long userId) {
        return Result.success(null);
      }
    };
  }
}
