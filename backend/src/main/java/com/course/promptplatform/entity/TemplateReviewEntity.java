package com.course.promptplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("template_review")
public class TemplateReviewEntity {

    @TableId(value = "review_id", type = IdType.AUTO)
    private Long reviewId;

    @TableField("user_id")
    private Long userId;

    @TableField("template_id")
    private Long templateId;

    @TableField("use_log_id")
    private Long useLogId;

    private Integer score;

    @TableField("review_content")
    private String reviewContent;

    @TableField("review_status")
    private String reviewStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
