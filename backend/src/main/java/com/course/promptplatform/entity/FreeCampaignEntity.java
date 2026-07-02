package com.course.promptplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("free_campaign")
public class FreeCampaignEntity {

    @TableId(value = "campaign_id", type = IdType.AUTO)
    private Long campaignId;

    @TableField("campaign_name")
    private String campaignName;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("campaign_status")
    private String campaignStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
