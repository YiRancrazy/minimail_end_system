package com.yirancrazy.minimall.platform.service;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.platform.dto.PlatformUserPageDto;
import com.yirancrazy.minimall.platform.vo.PlatformUserVo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端用户管理业务契约，提供用户分页和详情查询.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface PlatformUserService {

  /**
   * 按游标和关键词查询平台用户列表.
   *
   * @param dto 用户分页筛选条件
   * @return 用户视图分页；远程结果为空时返回空页
   */
  CursorPageVo<PlatformUserVo> page(PlatformUserPageDto dto);

  /**
   * 查询指定平台用户的详情并转换展示字段.
   *
   * @param userId 用户 ID
   * @return 用户管理视图
   * @throws com.yirancrazy.minimall.common.exception.BizException 用户服务未返回用户数据时抛出
   */
  PlatformUserVo detail(Long userId);
}
