package com.yirancrazy.minimall.common.config;

import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MybatisPlusGlobalConfig 配置类。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Configuration
public class MybatisPlusGlobalConfig {

    /**
     * 执行 mybatisPlusInterceptor 方法。
     * @return 处理结果
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor i = new MybatisPlusInterceptor();
        i.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        i.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return i;
    }


            /**
             * 执行 insertFill 方法。
             * @param m m 参数
             */
    /**
     * 执行 metaObjectHandler 方法。
     * @return 处理结果
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            /**
             * 创建填充。
             * @param m m
             */
            public void insertFill(MetaObject m) {
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(m, "createTime", LocalDateTime.class, now);
                strictInsertFill(m, "updateTime", LocalDateTime.class, now);
                strictInsertFill(m, "isDeleted", Integer.class, 0);
            }


            /**
             * 更新业务数据。
             * @param m m 参数
             */
            public void updateFill(MetaObject m) {
                strictUpdateFill(m, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
