package com.yirancrazy.minimall.platform.service.impl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.platform.dto.PlatformMerchantPageDTO;
import com.yirancrazy.minimall.platform.vo.PlatformMerchantVO;

/**
 * PlatformMerchantServiceImpl 单元测试，覆盖分页占位、详情占位的契约行为。
 */
public class PlatformMerchantServiceImplTest {

    private final PlatformMerchantServiceImpl service = new PlatformMerchantServiceImpl();

    /**
     * 验证分页占位实现返回空游标分页。
     */
    @Test
    public void page_returns_empty_placeholder() {
        PlatformMerchantPageDTO dto = new PlatformMerchantPageDTO();
        dto.setLimit(20);

        CursorPageVO<PlatformMerchantVO> page = service.page(dto);

        assertFalse(page.isHasMore());
        assertEquals(0, page.getRecords().size());
        assertEquals(20, page.getLimit());
    }

    /**
     * 验证详情占位实现抛 PLATFORM_MERCHANT_NOT_FOUND。
     */
    @Test
    public void detail_throws_not_found() {
        assertThrows(BizException.class, () -> service.detail(1L));
    }
}
