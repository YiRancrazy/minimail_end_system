package com.yirancrazy.minimall.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 注册 MyBatis-Plus 分页与乐观锁插件，并提供审计字段自动填充处理器。
 * 分页固定使用 MySQL 方言，创建时间、更新时间与逻辑删除初始值由框架自动维护。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Configuration
public class MybatisPlusGlobalConfig {

    /**
     * 创建 MyBatis-Plus 拦截器链，依次启用 MySQL 分页和乐观锁插件。
     *
     * @return 已配置分页与乐观锁的拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor i = new MybatisPlusInterceptor();
        i.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        i.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return i;
    }

    /**
     * 创建审计字段自动填充处理器，插入时填充创建时间、更新时间和未删除标记，更新时刷新更新时间。
     *
     * @return MyBatis-Plus 元对象处理器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        /* 匿名实现只负责 MyBatis-Plus 审计字段填充，不向外暴露额外状态。 */
        return new MetaObjectHandler() {
            /**
             * 在插入前填充审计字段，并仅在目标字段为空时写入。
             *
             * @param m MyBatis-Plus 当前持久化对象的元数据
             */
            public void insertFill(MetaObject m) {
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(m, "createTime", LocalDateTime.class, now);
                strictInsertFill(m, "updateTime", LocalDateTime.class, now);
                strictInsertFill(m, "isDeleted", Integer.class, 0);
            }

            /**
             * 在更新前刷新更新时间，并仅在目标字段为空时写入。
             *
             * @param m MyBatis-Plus 当前持久化对象的元数据
             */
            public void updateFill(MetaObject m) {
                strictUpdateFill(m, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
