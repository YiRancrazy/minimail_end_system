package com.yirancrazy.minimall.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 所有持久化对象共享的基础字段定义，统一主键、审计时间、逻辑删除和乐观锁列。
 * 子类只需映射业务列，字段填充策略由 MyBatis-Plus MetaObjectHandler 统一执行。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public abstract class BasePo implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "NULL")
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;

    @Version private Integer version;
}
