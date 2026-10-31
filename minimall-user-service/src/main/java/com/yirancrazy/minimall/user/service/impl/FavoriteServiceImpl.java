package com.yirancrazy.minimall.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.user.constant.UserCodeEnum;
import com.yirancrazy.minimall.user.dto.FavoritePageDto;
import com.yirancrazy.minimall.user.entity.UserFavoritePo;
import com.yirancrazy.minimall.user.gateway.FavoriteRemoteGateway;
import com.yirancrazy.minimall.user.manager.UserFavoriteManager;
import com.yirancrazy.minimall.user.service.FavoriteService;
import com.yirancrazy.minimall.user.vo.FavoriteVo;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户收藏业务实现，负责参数校验、顺序幂等新增、游标分页、商品快照批量富化及服务不可用降级.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {

  private final UserFavoriteManager userFavoriteManager;
  private final FavoriteRemoteGateway favoriteRemoteGateway;
  private final MinioUtil minioUtil;

  /**
   * 注入收藏持久化、商品远程查询和头像 URL 解析依赖。
   *
   * @param userFavoriteManager 用户收藏 Manager
   * @param favoriteRemoteGateway 商品快照查询边界
   * @param minioUtil 商品主图 URL 解析工具
   */
  public FavoriteServiceImpl(
      UserFavoriteManager userFavoriteManager,
      FavoriteRemoteGateway favoriteRemoteGateway,
      MinioUtil minioUtil) {
    this.userFavoriteManager = userFavoriteManager;
    this.favoriteRemoteGateway = favoriteRemoteGateway;
    this.minioUtil = minioUtil;
  }

  /**
   * 新增用户商品收藏。
   *
   * <p>先校验 ID 为正数，再按用户和 SKU 查询已有关系；已存在时直接返回，否则写入一条收藏记录。
   *
   * @param userId 用户 ID
   * @param skuId SKU ID
   * @throws BizException 任一 ID 为空或非正数时抛出
   */
  public void addFavorite(Long userId, Long skuId) {
    if (userId == null || userId <= 0 || skuId == null || skuId <= 0) {
      throw new BizException(UserCodeEnum.PARAM_INVALID);
    }
    long exists =
        userFavoriteManager.count(
            Wrappers.lambdaQuery(UserFavoritePo.class)
                .eq(UserFavoritePo::getUserId, userId)
                .eq(UserFavoritePo::getSkuId, skuId));
    if (exists > 0) {
      log.info("收藏已存在，userId={}，skuId={}", userId, skuId);
      return;
    }
    UserFavoritePo po = new UserFavoritePo();
    po.setUserId(userId);
    po.setSkuId(skuId);
    userFavoriteManager.save(po);
    log.info("收藏已添加，userId={}，skuId={}", userId, skuId);
  }

  /**
   * 按用户 ID 和 SKU ID 条件删除收藏。
   *
   * <p>参数必须为正数；收藏关系不存在时删除结果为 0，方法仍正常结束。
   *
   * @param userId 用户 ID
   * @param skuId SKU ID
   * @throws BizException 任一 ID 为空或非正数时抛出
   */
  public void removeFavorite(Long userId, Long skuId) {
    if (userId == null || userId <= 0 || skuId == null || skuId <= 0) {
      throw new BizException(UserCodeEnum.PARAM_INVALID);
    }
    userFavoriteManager.remove(
        Wrappers.lambdaQuery(UserFavoritePo.class)
            .eq(UserFavoritePo::getUserId, userId)
            .eq(UserFavoritePo::getSkuId, skuId));
    log.info("收藏已移除，userId={}，skuId={}", userId, skuId);
  }

  /**
   * 使用游标查询收藏，并仅对当前页记录补充 SKU 和 SPU 展示信息。
   *
   * <p>收藏记录按主键倒序读取。商品快照查询为空或服务降级时，收藏记录仍返回，商品字段保留为空。
   *
   * @param userId 用户 ID
   * @param dto 游标和每页数量
   * @return 收藏视图游标分页结果
   */
  @Override
  public CursorPageVo<FavoriteVo> pageFavorites(Long userId, FavoritePageDto dto) {
    Long lastId = CursorUtils.decode(dto.getCursor());
    int limit = dto.getLimit();
    List<UserFavoritePo> records =
        userFavoriteManager.list(
            Wrappers.lambdaQuery(UserFavoritePo.class)
                .lt(lastId != null, UserFavoritePo::getId, lastId)
                .eq(UserFavoritePo::getUserId, userId)
                .orderByDesc(UserFavoritePo::getId)
                .last("LIMIT " + (limit + 1)));
    CursorPageVo<UserFavoritePo> page = CursorPageVo.of(records, limit, UserFavoritePo::getId);

    // 仅富化当前页记录，避免为 hasMore 探测行额外拉取商品快照
    List<Long> skuIds =
        page.getRecords().stream()
            .map(UserFavoritePo::getSkuId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    Map<Long, SkuSnapshotDto> skuMap = fetchSkuMap(skuIds);
    List<Long> spuIds =
        skuMap.values().stream()
            .map(SkuSnapshotDto::getSpuId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    Map<Long, SpuSnapshotDto> spuMap = fetchSpuMap(spuIds);
    return page.map(po -> toVo(po, skuMap, spuMap));
  }

  /**
   * 批量读取当前页 SKU 快照，并将空响应转换为空 Map。
   *
   * @param skuIds 去重后的 SKU ID 列表
   * @return 以 SKU ID 为键的快照映射；无查询项或响应无数据时返回空 Map
   */
  private Map<Long, SkuSnapshotDto> fetchSkuMap(List<Long> skuIds) {
    if (skuIds == null || skuIds.isEmpty()) {
      return Map.of();
    }
    // 商品服务不可用或返回异常时由 fallback 兜底为空 Map，收藏记录仍可展示（商品字段留空）
    return Optional.ofNullable(favoriteRemoteGateway.batchSkuSnapshot(skuIds).getData())
        .orElseGet(Map::of);
  }

  /**
   * 批量读取当前页 SPU 快照，并将空响应转换为空 Map。
   *
   * @param spuIds 去重后的 SPU ID 列表
   * @return 以 SPU ID 为键的快照映射；无查询项或响应无数据时返回空 Map
   */
  private Map<Long, SpuSnapshotDto> fetchSpuMap(List<Long> spuIds) {
    if (spuIds == null || spuIds.isEmpty()) {
      return Map.of();
    }
    return Optional.ofNullable(favoriteRemoteGateway.batchSpuSnapshot(spuIds).getData())
        .orElseGet(Map::of);
  }

  /**
   * 组合收藏关系和商品快照，生成收藏列表视图。
   *
   * <p>SKU 名称缺失时回退使用 SPU 标题；SPU 存在时将主图对象键解析为公开 URL。
   *
   * @param po 收藏持久化对象
   * @param skuMap SKU 快照映射
   * @param spuMap SPU 快照映射
   * @return 收藏视图；商品快照缺失时仅保留收藏基础字段
   */
  private FavoriteVo toVo(
      UserFavoritePo po, Map<Long, SkuSnapshotDto> skuMap, Map<Long, SpuSnapshotDto> spuMap) {
    FavoriteVo vo = new FavoriteVo();
    vo.setId(po.getId());
    vo.setSkuId(po.getSkuId());
    vo.setCreateTime(po.getCreateTime());
    SkuSnapshotDto sku = skuMap.get(po.getSkuId());
    SpuSnapshotDto spu = sku == null || sku.getSpuId() == null ? null : spuMap.get(sku.getSpuId());
    if (sku != null) {
      vo.setSpuId(sku.getSpuId());
      vo.setSkuName(sku.getSkuName());
      vo.setPrice(sku.getPrice());
    }
    if (spu != null) {
      // SPU 标题用于在 SKU 名称缺失时回退展示商品名；主图 objectKey 转可访问 URL
      if (vo.getSkuName() == null) {
        vo.setSkuName(spu.getTitle());
      }
      vo.setSkuImage(minioUtil.resolvePublicUrl(spu.getMainImageUrl()));
    }
    return vo;
  }

  /**
   * 统计指定用户的收藏记录数量。
   *
   * @param userId 用户 ID
   * @return 收藏数量；无记录时为 0
   */
  @Override
  public int countFavorites(Long userId) {
    return Math.toIntExact(
        userFavoriteManager.count(
            Wrappers.lambdaQuery(UserFavoritePo.class).eq(UserFavoritePo::getUserId, userId)));
  }
}
