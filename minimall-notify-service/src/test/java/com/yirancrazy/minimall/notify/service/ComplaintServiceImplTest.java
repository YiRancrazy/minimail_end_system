package com.yirancrazy.minimall.notify.service;

import java.util.Collections;
import java.util.List;
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
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.notify.constant.ComplaintStatusEnum;
import com.yirancrazy.minimall.notify.constant.ComplaintTypeEnum;
import com.yirancrazy.minimall.notify.dto.ComplaintCreateDTO;
import com.yirancrazy.minimall.notify.dto.ComplaintHandleDTO;
import com.yirancrazy.minimall.notify.dto.ComplaintPageDTO;
import com.yirancrazy.minimall.notify.entity.ComplaintPO;
import com.yirancrazy.minimall.notify.manager.ComplaintManager;
import com.yirancrazy.minimall.notify.service.impl.ComplaintServiceImpl;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ComplaintServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class ComplaintServiceImplTest {

    private ComplaintManager complaintManager;
    private ComplaintServiceImpl service;

    @BeforeEach
    void setUp() {
        // 初始化 TableInfo，便于 LambdaQueryWrapper 解析 lambda 缓存
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
     * 创建业务数据。
     */
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
     * 创建业务数据。
     */
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
     * 分页查询数据。
     */
    public void page_delegates_to_manager() {
        ComplaintPageDTO dto = new ComplaintPageDTO();
        dto.setLimit(10);

        List<ComplaintPO> records = Collections.emptyList();
        when(complaintManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(records);

        CursorPageVO<ComplaintPO> result = service.page(dto);

        assertNotNull(result);
        verify(complaintManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }


    /**
     * 分页查询数据。
     */
    public void page_filters_by_status_and_order_no() {
        ComplaintPageDTO dto = new ComplaintPageDTO();
        dto.setLimit(10);
        dto.setStatus(ComplaintStatusEnum.PENDING.intCode());
        dto.setOrderNo("ORDER001");

        List<ComplaintPO> records = Collections.emptyList();
        when(complaintManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(records);

        CursorPageVO<ComplaintPO> result = service.page(dto);

        assertNotNull(result);
        verify(complaintManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }


    /**
     * 处理业务事件。
     */
    public void handle_pending_to_processing() {
        ComplaintPO po = new ComplaintPO();
        po.setId(1L);
        po.setStatus(ComplaintStatusEnum.PENDING.intCode());
        when(complaintManager.getById(1L)).thenReturn(po);
        when(complaintManager.update(any(Wrapper.class))).thenReturn(true);

        ComplaintHandleDTO dto = new ComplaintHandleDTO();
        dto.setStatus(ComplaintStatusEnum.PROCESSING.intCode());
        dto.setResult("正在核实");

        service.handle(1L, 10L, dto);

        verify(complaintManager).update(any(Wrapper.class));
        verify(complaintManager, never()).updateById(any(ComplaintPO.class));
    }


    /**
     * 处理业务事件。
     */
    public void handle_processing_to_resolved() {
        ComplaintPO po = new ComplaintPO();
        po.setId(1L);
        po.setStatus(ComplaintStatusEnum.PROCESSING.intCode());
        when(complaintManager.getById(1L)).thenReturn(po);
        when(complaintManager.update(any(Wrapper.class))).thenReturn(true);

        ComplaintHandleDTO dto = new ComplaintHandleDTO();
        dto.setStatus(ComplaintStatusEnum.RESOLVED.intCode());
        dto.setResult("已退款处理");

        service.handle(1L, 10L, dto);

        verify(complaintManager).update(any(Wrapper.class));
    }


    /**
     * 处理业务事件。
     */
    public void handle_processing_to_rejected() {
        ComplaintPO po = new ComplaintPO();
        po.setId(1L);
        po.setStatus(ComplaintStatusEnum.PROCESSING.intCode());
        when(complaintManager.getById(1L)).thenReturn(po);
        when(complaintManager.update(any(Wrapper.class))).thenReturn(true);

        ComplaintHandleDTO dto = new ComplaintHandleDTO();
        dto.setStatus(ComplaintStatusEnum.REJECTED.intCode());
        dto.setResult("证据不足");

        service.handle(1L, 10L, dto);

        verify(complaintManager).update(any(Wrapper.class));
    }


    /**
     * 处理业务事件。
     */
    public void handle_throws_when_conditional_update_affects_zero_rows() {
        ComplaintPO po = new ComplaintPO();
        po.setId(1L);
        po.setStatus(ComplaintStatusEnum.PENDING.intCode());
        when(complaintManager.getById(1L)).thenReturn(po);
        when(complaintManager.update(any(Wrapper.class))).thenReturn(false);

        ComplaintHandleDTO dto = new ComplaintHandleDTO();
        dto.setStatus(ComplaintStatusEnum.PROCESSING.intCode());
        dto.setResult("处理中");

        assertThrows(BizException.class, () -> service.handle(1L, 10L, dto));
    }


    /**
     * 处理业务事件。
     */
    public void handle_throws_on_invalid_transition() {
        ComplaintPO po = new ComplaintPO();
        po.setId(1L);
        po.setStatus(ComplaintStatusEnum.RESOLVED.intCode());
        when(complaintManager.getById(1L)).thenReturn(po);

        ComplaintHandleDTO dto = new ComplaintHandleDTO();
        dto.setStatus(ComplaintStatusEnum.PENDING.intCode());
        dto.setResult("非法操作");

        assertThrows(BizException.class, () -> service.handle(1L, 10L, dto));
        verify(complaintManager, never()).update(any(Wrapper.class));
    }


    /**
     * 处理业务事件。
     */
    public void handle_throws_when_not_found() {
        when(complaintManager.getById(999L)).thenReturn(null);

        ComplaintHandleDTO dto = new ComplaintHandleDTO();
        dto.setStatus(ComplaintStatusEnum.PROCESSING.intCode());
        dto.setResult("处理中");

        assertThrows(BizException.class, () -> service.handle(999L, 10L, dto));
    }


    /**
     * 处理业务事件。
     */
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
     * 处理业务事件。
     */
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


    /**
     * 执行 detail_returns_po 方法。
     */
    public void detail_returns_po() {
        ComplaintPO po = new ComplaintPO();
        po.setId(7L);
        when(complaintManager.getById(7L)).thenReturn(po);

        ComplaintPO got = service.detail(7L);

        assertEquals(7L, got.getId());
    }


    /**
     * 执行 detail_throws_when_not_found 方法。
     */
    public void detail_throws_when_not_found() {
        when(complaintManager.getById(99L)).thenReturn(null);

        assertThrows(BizException.class, () -> service.detail(99L));
    }


    /**
     * 执行 detailForUser_returns_own_complaint 方法。
     */
    public void detailForUser_returns_own_complaint() {
        ComplaintPO po = new ComplaintPO();
        po.setId(7L);
        po.setComplainantId(100L);
        when(complaintManager.getById(7L)).thenReturn(po);

        assertEquals(7L, service.detailForUser(7L, 100L).getId());
    }


    /**
     * 执行 detailForUser_throws_when_not_owner 方法。
     */
    public void detailForUser_throws_when_not_owner() {
        ComplaintPO po = new ComplaintPO();
        po.setId(7L);
        po.setComplainantId(200L);
        when(complaintManager.getById(7L)).thenReturn(po);

        assertThrows(BizException.class, () -> service.detailForUser(7L, 100L));
    }
}
