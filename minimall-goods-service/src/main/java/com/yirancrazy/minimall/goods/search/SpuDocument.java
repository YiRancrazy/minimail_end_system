package com.yirancrazy.minimall.goods.search;

import java.time.Instant;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义 minimall_spu 索引中的商品搜索字段、价格分值和中文分词映射。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@Document(indexName = "minimall_spu")
@Setting(shards = 1, replicas = 0)
public class SpuDocument {

  @Id private Long spuId;

  @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
  private String title;

  @Field(type = FieldType.Long)
  private Long categoryId;

  @Field(type = FieldType.Long)
  private Long merchantId;

  @Field(type = FieldType.Long)
  private Long minPrice;

  @Field(type = FieldType.Long)
  private Long maxPrice;

  @Field(type = FieldType.Integer)
  private Integer saleStatus;

  @Field(type = FieldType.Keyword)
  private String mainImage;

  @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
  private Instant createTime;
}
