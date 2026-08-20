package com.yirancrazy.minimall.goods.config;

import java.util.Map;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.goods.search.SpuDocument;
import com.yirancrazy.minimall.goods.service.SpuService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Elasticsearch 配置类，启用 ES Repository 支持并确保索引 mapping 与全量数据就绪
 * @Version: 1.1
 * @DateTime: 2026/08/19
 **/
@Slf4j
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.yirancrazy.minimall.goods.search")
public class ElasticsearchConfig {

    /**
     * 启动后初始化 ES 索引：索引缺失或 title 未按 ik 分词时重建 mapping，
     * 随后全量重灌在售 SPU 文档。避免首次建索引时 Spring Data 沿用默认 standard 分词导致中文搜不到。
     * @param elasticsearchOperations ES 操作入口
     * @param spuService 商品领域服务，提供全量重灌能力
     * @return 应用启动完成后的初始化任务
     */
    @Bean
    public ApplicationRunner esIndexInitializer(ElasticsearchOperations elasticsearchOperations,
                                                SpuService spuService) {
        return args -> {
            IndexOperations ops = elasticsearchOperations.indexOps(SpuDocument.class);
            if (ops.exists() && hasIkAnalyzer(ops)) {
                log.info("ES index {} already has ik analyzer, skip recreate",
                    SpuDocument.class.getSimpleName());
            }
            else {
                // 索引缺失或缺 ik 分词器 → 重建，保证 title 中文分词生效
                if (ops.exists()) {
                    ops.delete();
                    log.warn("ES index recreated due to missing ik analyzer");
                }
                ops = elasticsearchOperations.indexOps(SpuDocument.class);
                ops.create();
                ops.putMapping(ops.createMapping());
                log.info("ES index created with ik mapping");
            }
            spuService.rebuildAllEsDocuments();
        };
    }

    /**
     * 判断现有索引的 title 字段是否已按 ik_max_word 分词。
     * @param ops 索引操作入口
     * @return true 表示 title 已配置 ik 分析器
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
