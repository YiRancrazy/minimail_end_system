package com.yirancrazy.minimall.user.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.user.constant.UserCodeEnum;
import com.yirancrazy.minimall.user.dto.FavoritePageDTO;
import com.yirancrazy.minimall.user.entity.UserFavoritePO;
import com.yirancrazy.minimall.user.manager.UserFavoriteManager;
import com.yirancrazy.minimall.user.service.FavoriteService;
import com.yirancrazy.minimall.user.vo.FavoriteVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户收藏领域服务实现，实现商品收藏增删查业务逻辑
 * @Version: 2.0
 * @DateTime: 2026/08/20
 */
@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final UserFavoriteManager userFavoriteManager;
    private final GoodsFeignClient goodsFeignClient;
    private final MinioUtil minioUtil;

    public FavoriteServiceImpl(UserFavoriteManager userFavoriteManager, GoodsFeignClient goodsFeignClient,
                               MinioUtil minioUtil) {
        this.userFavoriteManager = userFavoriteManager;
        this.goodsFeignClient = goodsFeignClient;
        this.minioUtil = minioUtil;
    }

    /**
     * 收藏商品，已存在则幂等返回。
     *
     * @param userId 用户ID，必须 > 0
     * @param skuId 商品SKU ID，必须 > 0
     */
    @Override
    public void addFavorite(Long userId, Long skuId) {
        if (userId == null || userId <= 0 || skuId == null || skuId <= 0) {
            throw new BizException(UserCodeEnum.PARAM_INVALID);
        }
        long exists = userFavoriteManager.count(Wrappers.lambdaQuery(UserFavoritePO.class)
            .eq(UserFavoritePO::getUserId, userId)
            .eq(UserFavoritePO::getSkuId, skuId));
        if (exists > 0) {
            log.info("favorite already exists, userId={}, skuId={}", userId, skuId);
            return;
        }
        UserFavoritePO po = new UserFavoritePO();
        po.setUserId(userId);
        po.setSkuId(skuId);
        userFavoriteManager.save(po);
        log.info("favorite added, userId={}, skuId={}", userId, skuId);
    }

    /**
     * 取消收藏，不存在则幂等返回。
     *
     * @param userId 用户ID，必须 > 0
     * @param skuId 商品SKU ID，必须 > 0
     */
    @Override
    public void removeFavorite(Long userId, Long skuId) {
        if (userId == null || userId <= 0 || skuId == null || skuId <= 0) {
            throw new BizException(UserCodeEnum.PARAM_INVALID);
        }
        userFavoriteManager.remove(Wrappers.lambdaQuery(UserFavoritePO.class)
            .eq(UserFavoritePO::getUserId, userId)
            .eq(UserFavoritePO::getSkuId, skuId));
        log.info("favorite removed, userId={}, skuId={}", userId, skuId);
    }

    /**
     * 分页查询用户收藏列表，按收藏时间倒序。
     *
     * @param userId 用户ID
     * @param dto 分页入参
     * @return 收藏分页结果
     */
    @Override
    public CursorPageVO<FavoriteVO> pageFavorites(Long userId, FavoritePageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<UserFavoritePO> records = userFavoriteManager.list(Wrappers.lambdaQuery(UserFavoritePO.class)
            .lt(lastId != null, UserFavoritePO::getId, lastId)
            .eq(UserFavoritePO::getUserId, userId)
            .orderByDesc(UserFavoritePO::getId)
            .last("LIMIT " + (limit + 1)));
        CursorPageVO<UserFavoritePO> page = CursorPageVO.of(records, limit, UserFavoritePO::getId);

        // 仅富化当前页记录，避免为 hasMore 探测行额外拉取商品快照
        List<Long> skuIds = page.getRecords().stream()
            .map(UserFavoritePO::getSkuId).filter(Objects::nonNull).distinct().toList();
        Map<Long, SkuSnapshotDTO> skuMap = fetchSkuMap(skuIds);
        List<Long> spuIds = skuMap.values().stream()
            .map(SkuSnapshotDTO::getSpuId).filter(Objects::nonNull).distinct().toList();
        Map<Long, SpuSnapshotDTO> spuMap = fetchSpuMap(spuIds);
        return page.map(po -> toVO(po, skuMap, spuMap));
    }

    private Map<Long, SkuSnapshotDTO> fetchSkuMap(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Map.of();
        }
        // 商品服务不可用或返回异常时由 fallback 兜底为空 Map，收藏记录仍可展示（商品字段留空）
        return Optional.ofNullable(goodsFeignClient.batchSkuSnapshot(skuIds).getData()).orElseGet(Map::of);
    }

    private Map<Long, SpuSnapshotDTO> fetchSpuMap(List<Long> spuIds) {
        if (spuIds == null || spuIds.isEmpty()) {
            return Map.of();
        }
        return Optional.ofNullable(goodsFeignClient.batchSpuSnapshot(spuIds).getData()).orElseGet(Map::of);
    }

    private FavoriteVO toVO(UserFavoritePO po, Map<Long, SkuSnapshotDTO> skuMap,
                            Map<Long, SpuSnapshotDTO> spuMap) {
        FavoriteVO vo = new FavoriteVO();
        vo.setId(po.getId());
        vo.setSkuId(po.getSkuId());
        vo.setCreateTime(po.getCreateTime());
        SkuSnapshotDTO sku = skuMap.get(po.getSkuId());
        SpuSnapshotDTO spu = sku == null || sku.getSpuId() == null ? null : spuMap.get(sku.getSpuId());
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
     * 统计用户收藏数量。
     *
     * @param userId 用户ID
     * @return 收藏数量
     */
    @Override
    public int countFavorites(Long userId) {
        return Math.toIntExact(userFavoriteManager.count(Wrappers.lambdaQuery(UserFavoritePO.class)
            .eq(UserFavoritePO::getUserId, userId)));
    }
}
