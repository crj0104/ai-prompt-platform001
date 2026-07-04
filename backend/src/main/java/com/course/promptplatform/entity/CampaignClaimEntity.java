package com.course.promptplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("campaign_claim")
public class CampaignClaimEntity {

    @TableId(value = "claim_id", type = IdType.AUTO)
    private Long claimId;

    @TableField("campaign_template_id")
    private Long campaignTemplateId;

    @TableField("user_id")
    private Long userId;

    @TableField("order_id")
    private Long orderId;

    @TableField("claim_status")
    private String claimStatus;

    @TableField("claim_time")
    private LocalDateTime claimTime;
}
