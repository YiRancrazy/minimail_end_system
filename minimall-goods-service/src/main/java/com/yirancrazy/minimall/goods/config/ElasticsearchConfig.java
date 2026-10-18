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
 * @Description: ElasticsearchConfig 配置类。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.yirancrazy.minimall.goods.search")
public class ElasticsearchConfig {


    /**
     * 执行 esIndexInitializer 方法。
     * @param elasticsearchOperations elasticsearchOperations 参数
     * @param spuService spuService 参数
     * @return 处理结果
     */
    @Bean
    public ApplicationRunner esIndexInitializer(ElasticsearchOperations elasticsearchOperations,
                                                SpuService spuService) {
        return args -> {
            IndexOperations ops = elasticsearchOperations.indexOps(SpuDocument.class);
            if (ops.exists() && hasIkAnalyzer(ops)) {
                log.info("ES 索引 {} 已包含 ik 分词器，跳过重建",
                    SpuDocument.class.getSimpleName());
            }
            else {
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
     * 校验IkAnalyzer。
     * @param ops ops
     * @return 是否满足条件
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
