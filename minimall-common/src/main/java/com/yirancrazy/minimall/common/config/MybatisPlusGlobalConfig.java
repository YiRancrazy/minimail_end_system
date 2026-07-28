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

@Configuration
public class MybatisPlusGlobalConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor i = new MybatisPlusInterceptor();
        i.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        i.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return i;
    }

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

            @Override
            public void updateFill(MetaObject m) {
                strictUpdateFill(m, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}