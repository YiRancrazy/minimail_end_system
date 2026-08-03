package com.yirancrazy.minimall.goods.search;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品 SPU Elasticsearch 文档对象，映射 minimall_spu 索引
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@Document(indexName = "minimall_spu")
@Setting(shards = 1, replicas = 0)
public class SpuDocument {

    @Id
    private Long spuId;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Long)
    private Long merchantId;

    /** 最低价格，scaled_float scalingFactor=100，存储分为 Long */
    @Field(type = FieldType.Long)
    private Long minPrice;

    /** 最高价格，scaled_float scalingFactor=100，存储分为 Long */
    @Field(type = FieldType.Long)
    private Long maxPrice;

    /** 销售状态，2=在售 */
    @Field(type = FieldType.Integer)
    private Integer saleStatus;

    @Field(type = FieldType.Keyword)
    private String mainImage;

    @Field(type = FieldType.Date)
    private LocalDateTime createTime;
}
