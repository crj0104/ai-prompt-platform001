package com.course.promptplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 模板主表实体，供 MyBatis-Plus 直接映射与分页查询。
 */
@Data
@TableName("prompt_template")
public class PromptTemplateEntity {

    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    @TableField("creator_user_id")
    private Long creatorUserId;

    private String title;

    @TableField("scene_desc")
    private String sceneDesc;

    @TableField("current_version_id")
    private Long currentVersionId;

    @TableField("price_type")
    private String priceType;

    private BigDecimal price;

    @TableField("shelf_status")
    private String shelfStatus;

    @TableField("use_count")
    private Integer useCount;

    @TableField("favorite_count")
    private Integer favoriteCount;

    @TableField("avg_score")
    private Double avgScore;

    @TableField("review_count")
    private Integer reviewCount;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
