package com.course.promptplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("template_use_log")
public class TemplateUseLogEntity {

    @TableId(value = "use_log_id", type = IdType.AUTO)
    private Long useLogId;

    @TableField("user_id")
    private Long userId;

    @TableField("template_id")
    private Long templateId;

    @TableField("version_id")
    private Long versionId;

    @TableField("order_id")
    private Long orderId;

    @TableField("input_summary")
    private String inputSummary;

    @TableField("use_source")
    private String useSource;

    @TableField("used_at")
    private LocalDateTime usedAt;
}
