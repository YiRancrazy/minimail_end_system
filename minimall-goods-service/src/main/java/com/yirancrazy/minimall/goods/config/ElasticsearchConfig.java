package com.yirancrazy.minimall.goods.config;

import com.yirancrazy.minimall.goods.search.SpuDocument;
import com.yirancrazy.minimall.goods.service.SpuService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 初始化 SPU 搜索索引，校验 ik 分词映射，并在启动后重建在售商品文档。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.yirancrazy.minimall.goods.search")
public class ElasticsearchConfig {

    /**
     * 初始化 SPU 搜索索引：复用含 ik_max_word 映射的索引，否则删除重建，随后回填全部在售商品文档。
     *
     * @param elasticsearchOperations Elasticsearch 查询与索引操作入口
     * @param spuService SPU 聚合服务
     * @return 应用启动完成后执行的索引初始化任务
     */
    @Bean
    public ApplicationRunner esIndexInitializer(
            ElasticsearchOperations elasticsearchOperations, SpuService spuService) {
        return args -> {
            IndexOperations ops = elasticsearchOperations.indexOps(SpuDocument.class);
            if (ops.exists() && hasIkAnalyzer(ops)) {
                log.info("ES 索引 {} 已包含 ik 分词器，跳过重建", SpuDocument.class.getSimpleName());
            } else {
                // 索引缺失或缺 ik 分词器 → 重建，保证 title 中文分词生效
                if (ops.exists()) {
                    ops.delete();
                    log.warn("ES 索引缺少 ik 分词器，已重建");
                }
                ops = elasticsearchOperations.indexOps(SpuDocument.class);
                ops.create();
                ops.putMapping(ops.createMapping());
                log.info("ES 索引已使用 ik mapping 创建");
            }
            spuService.rebuildAllEsDocuments();
        };
    }

    /**
     * 检查 title 字段是否使用 ik_max_word 分词器，供索引重建判断。
     *
     * @param ops SPU 索引操作句柄
     * @return 索引 mapping 中 title.analyzer 为 ik_max_word 时返回 true
     */
    private boolean hasIkAnalyzer(IndexOperations ops) {
        Map<String, Object> mapping = ops.getMapping();
        if (mapping == null) {
            return false;
        }
        Object properties = mapping.get("properties");
        if (!(properties instanceof Map)) {
            return false;
        }
        Object title = ((Map<?, ?>) properties).get("title");
        if (!(title instanceof Map)) {
            return false;
        }
        return "ik_max_word".equals(((Map<?, ?>) title).get("analyzer"));
    }
}
