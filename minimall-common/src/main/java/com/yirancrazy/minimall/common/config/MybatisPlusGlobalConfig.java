package com.yirancrazy.minimall.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MyBatis-Plus 全局配置，注册 MySQL 分页插件与乐观锁插件，并统一实现创建时间、更新时间和逻辑删除标记的字段自动填充。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Configuration
public class MybatisPlusGlobalConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor i = new MybatisPlusInterceptor();
        i.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        i.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return i;
    }

    /**
     * 注册元数据自动填充处理器，统一维护公共审计字段。
     *
     * @return 元数据填充处理器，插入时填充 createTime、updateTime 与 isDeleted，更新时填充 updateTime
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject m) {
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(m, "createTime", LocalDateTime.class, now);
                strictInsertFill(m, "updateTime", LocalDateTime.class, now);
                strictInsertFill(m, "isDeleted", Integer.class, 0);
            }

            /**
             * 更新操作时自动填充更新时间字段。
             *
             * @param m 当前待更新实体的元对象
             */
            @Override
            public void updateFill(MetaObject m) {
                strictUpdateFill(m, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}