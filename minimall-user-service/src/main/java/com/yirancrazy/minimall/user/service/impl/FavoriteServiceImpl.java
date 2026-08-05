package com.yirancrazy.minimall.user.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.user.constant.UserCodeEnum;
import com.yirancrazy.minimall.user.dto.FavoritePageDTO;
import com.yirancrazy.minimall.user.entity.UserFavoritePO;
import com.yirancrazy.minimall.user.manager.UserFavoriteManager;
import com.yirancrazy.minimall.user.service.FavoriteService;
import com.yirancrazy.minimall.user.vo.FavoriteVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户收藏领域服务实现，实现商品收藏增删查业务逻辑
 * @Version: 1.0
 * @DateTime: 2026/08/03
 */
@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final UserFavoriteManager userFavoriteManager;

    public FavoriteServiceImpl(UserFavoriteManager userFavoriteManager) {
        this.userFavoriteManager = userFavoriteManager;
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
        return CursorPageVO.of(records, limit, UserFavoritePO::getId).map(this::toVO);
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

    private FavoriteVO toVO(UserFavoritePO po) {
        return new FavoriteVO(po.getId(), po.getUserId(), po.getSkuId(), po.getCreateTime());
    }
}
