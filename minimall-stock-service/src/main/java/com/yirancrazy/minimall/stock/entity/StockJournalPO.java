package com.yirancrazy.minimall.stock.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockJournal持久化对象，映射t_stock_journal表
 * @Version: 1.0
 * @DateTime: 2026/07/31
 **/
@Data
@TableName("t_stock_journal")
public class StockJournalPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long skuId;
    private Long quantity;
    private Integer type;
    private String reason;
    private String orderNo;

    // BasePO fields redefined with the real column names used by V13 migration.
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;
}