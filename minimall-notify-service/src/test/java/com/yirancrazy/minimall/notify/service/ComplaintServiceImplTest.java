package com.yirancrazy.minimall.notify.service;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.ComplaintStatusEnum;
import com.yirancrazy.minimall.notify.constant.ComplaintTypeEnum;
import com.yirancrazy.minimall.notify.dto.ComplaintCreateDTO;
import com.yirancrazy.minimall.notify.dto.ComplaintHandleDTO;
import com.yirancrazy.minimall.notify.dto.ComplaintPageDTO;
import com.yirancrazy.minimall.notify.entity.ComplaintPO;
import com.yirancrazy.minimall.notify.manager.ComplaintManager;
import com.yirancrazy.minimall.notify.service.impl.ComplaintServiceImpl;

/**
 * ComplaintServiceImpl 单元测试，覆盖投诉提交、分页查询、处理的正常、失败、边界路径。
 */
public class ComplaintServiceImplTest {

    private ComplaintManager complaintManager;
    private ComplaintServiceImpl service;

    @BeforeEach
    void setUp() {
        // Initialize TableInfo so LambdaQueryWrapper can resolve lambda cache
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.notify.mapper.ComplaintMapper");
        TableInfoHelper.initTableInfo(assistant, ComplaintPO.class);

        complaintManager = mock(ComplaintManager.class);
        lenient().doAnswer(inv -> {
            ComplaintPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(complaintManager).save(any(ComplaintPO.class));
        lenient().when(complaintManager.updateById(any(ComplaintPO.class))).thenReturn(true);
        service = new ComplaintServiceImpl(complaintManager);
    }

    /**
     * 验证 create 正常提交投诉并返回 ID。
     */
    @Test
    public void create_persists_and_returns_id() {
        ComplaintCreateDTO dto = new ComplaintCreateDTO();
        dto.setComplainantType(1);
        dto.setComplainantId(100L);
        dto.setDefendantType(2);
        dto.setDefendantId(200L);
        dto.setOrderNo("ORDER001");
        dto.setComplaintType(ComplaintTypeEnum.QUALITY.getCode());
        dto.setTitle("商品质量问题");
        dto.setContent("收到的商品与描述不符");

        Long id = service.create(100L, dto);

        assertNotNull(id);
        verify(complaintManager).save(any(ComplaintPO.class));
    }

    /**
     * 验证 create 使用 X-User-Id 作为 complainantId，忽略 DTO 中的 complainantId。
     */
    @Test
    public void create_uses_header_complainant_id() {
        ComplaintCreateDTO dto = new ComplaintCreateDTO();
        dto.setComplainantType(1);
        dto.setComplainantId(999L);
        dto.setDefendantType(2);
        dto.setDefendantId(200L);
        dto.setComplaintType(ComplaintTypeEnum.QUALITY.getCode());
        dto.setTitle("测试");
        dto.setContent("内容");

        service.create(100L, dto);

        verify(complaintManager).save(any(ComplaintPO.class));
    }

    /**
     * 验证 page 分页查询委托给 manager。
     */
    @Test
    public void page_delegates_to_manager() {
        ComplaintPageDTO dto = new ComplaintPageDTO();
        dto.setPageNo(1);
        dto.setPageSize(10);

        IPage<ComplaintPO> expected = new Page<>(1, 10);
        when(complaintManager.page(any(IPage.class), any())).thenReturn(expected);

        IPage<ComplaintPO> result = service.page(dto);

        assertEquals(expected, result);
        verify(complaintManager).page(any(IPage.class), any());
    }

    /**
     * 验证 page 按 status 和 orderNo 过滤。
     */
    @Test
    public void page_filters_by_status_and_order_no() {
        ComplaintPageDTO dto = new ComplaintPageDTO();
        dto.setPageNo(1);
        dto.setPageSize(10);
        dto.setStatus(ComplaintStatusEnum.PENDING.intCode());
        dto.setOrderNo("ORDER001");

        IPage<ComplaintPO> expected = new Page<>(1, 10);
        when(complaintManager.page(any(IPage.class), any())).thenReturn(expected);

        IPage<ComplaintPO> result = service.page(dto);

        assertEquals(expected, result);
        verify(complaintManager).page(any(IPage.class), any());
    }

    /**
     * 验证 handle 正常处理投诉：PENDING → PROCESSING。
     */
    @Test
    public void handle_pending_to_processing() {
        ComplaintPO po = new ComplaintPO();
        po.setId(1L);
        po.setStatus(ComplaintStatusEnum.PENDING.intCode());
        when(complaintManager.getById(1L)).thenReturn(po);

        ComplaintHandleDTO dto = new ComplaintHandleDTO();
        dto.setStatus(ComplaintStatusEnum.PROCESSING.intCode());
        dto.setResult("正在核实");

        service.handle(1L, 10L, dto);

        assertEquals(ComplaintStatusEnum.PROCESSING.intCode(), po.getStatus());
        assertEquals(10L, po.getHandlerId());
        verify(complaintManager).updateById(po);
    }

    /**
     * 验证 handle 正常处理投诉：PROCESSING → RESOLVED。
     */
    @Test
    public void handle_processing_to_resolved() {
        ComplaintPO po = new ComplaintPO();
        po.setId(1L);
        po.setStatus(ComplaintStatusEnum.PROCESSING.intCode());
        when(complaintManager.getById(1L)).thenReturn(po);

        ComplaintHandleDTO dto = new ComplaintHandleDTO();
        dto.setStatus(ComplaintStatusEnum.RESOLVED.intCode());
        dto.setResult("已退款处理");

        service.handle(1L, 10L, dto);

        assertEquals(ComplaintStatusEnum.RESOLVED.intCode(), po.getStatus());
        verify(complaintManager).updateById(po);
    }

    /**
     * 验证 handle 正常处理投诉：PROCESSING → REJECTED。
     */
    @Test
    public void handle_processing_to_rejected() {
        ComplaintPO po = new ComplaintPO();
        po.setId(1L);
        po.setStatus(ComplaintStatusEnum.PROCESSING.intCode());
        when(complaintManager.getById(1L)).thenReturn(po);

        ComplaintHandleDTO dto = new ComplaintHandleDTO();
        dto.setStatus(ComplaintStatusEnum.REJECTED.intCode());
        dto.setResult("证据不足");

        service.handle(1L, 10L, dto);

        assertEquals(ComplaintStatusEnum.REJECTED.intCode(), po.getStatus());
        verify(complaintManager).updateById(po);
    }

    /**
     * 验证 handle 在状态非法转换时抛出 COMPLAINT_STATUS_INVALID。
     */
    @Test
    public void handle_throws_on_invalid_transition() {
        ComplaintPO po = new ComplaintPO();
        po.setId(1L);
        po.setStatus(ComplaintStatusEnum.RESOLVED.intCode());
        when(complaintManager.getById(1L)).thenReturn(po);

        ComplaintHandleDTO dto = new ComplaintHandleDTO();
        dto.setStatus(ComplaintStatusEnum.PENDING.intCode());
        dto.setResult("非法操作");

        assertThrows(BizException.class, () -> service.handle(1L, 10L, dto));
        verify(complaintManager, never()).updateById(any(ComplaintPO.class));
    }

    /**
     * 验证 handle 在投诉不存在时抛出 COMPLAINT_NOT_FOUND。
     */
    @Test
    public void handle_throws_when_not_found() {
        when(complaintManager.getById(999L)).thenReturn(null);

        ComplaintHandleDTO dto = new ComplaintHandleDTO();
        dto.setStatus(ComplaintStatusEnum.PROCESSING.intCode());
        dto.setResult("处理中");

        assertThrows(BizException.class, () -> service.handle(999L, 10L, dto));
    }

    /**
     * 验证 handle 不允许 PENDING 直接跳到 RESOLVED。
     */
    @Test
    public void handle_throws_on_pending_to_resolved() {
        ComplaintPO po = new ComplaintPO();
        po.setId(1L);
        po.setStatus(ComplaintStatusEnum.PENDING.intCode());
        when(complaintManager.getById(1L)).thenReturn(po);

        ComplaintHandleDTO dto = new ComplaintHandleDTO();
        dto.setStatus(ComplaintStatusEnum.RESOLVED.intCode());
        dto.setResult("跳过处理");

        assertThrows(BizException.class, () -> service.handle(1L, 10L, dto));
    }

    /**
     * 验证 handle 不允许 PENDING 直接跳到 REJECTED。
     */
    @Test
    public void handle_throws_on_pending_to_rejected() {
        ComplaintPO po = new ComplaintPO();
        po.setId(1L);
        po.setStatus(ComplaintStatusEnum.PENDING.intCode());
        when(complaintManager.getById(1L)).thenReturn(po);

        ComplaintHandleDTO dto = new ComplaintHandleDTO();
        dto.setStatus(ComplaintStatusEnum.REJECTED.intCode());
        dto.setResult("跳过处理");

        assertThrows(BizException.class, () -> service.handle(1L, 10L, dto));
    }
}
