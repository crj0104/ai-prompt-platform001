package com.course.promptplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("campaign_template")
public class CampaignTemplateEntity {

    @TableId(value = "campaign_template_id", type = IdType.AUTO)
    private Long campaignTemplateId;

    @TableField("campaign_id")
    private Long campaignId;

    @TableField("template_id")
    private Long templateId;

    @TableField("total_quota")
    private Integer totalQuota;

    @TableField("remaining_quota")
    private Integer remainingQuota;

    @TableField("per_user_limit")
    private Integer perUserLimit;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
