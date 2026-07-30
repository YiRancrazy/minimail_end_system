package com.yirancrazy.minimall.common.base;

import java.io.Serializable;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

/**
* 持久化实体基类，统一定义雪花算法主键、创建时间、更新时间和逻辑删除标记四个公共字段，并配置自动填充与逻辑删除注解。
 */
@Data
public abstract class BasePO implements Serializable
{
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;
}