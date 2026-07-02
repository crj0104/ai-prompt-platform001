package com.course.promptplatform.model.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 模板查询行对象，承接联表搜索与热门排行 SQL 的结果。
 */
@Data
public class TemplateQueryRow {

    private Long templateId;
    private Long creatorUserId;
    private String title;
    private String sceneDesc;
    private Long currentVersionId;
    private String priceType;
    private BigDecimal price;
    private String shelfStatus;
    private Integer useCount;
    private Integer favoriteCount;
    private Double avgScore;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String summary;
    private Integer recentUseCount;
    private BigDecimal hotScore;
}
