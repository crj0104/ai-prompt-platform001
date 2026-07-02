package com.course.promptplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_balance_log")
public class UserBalanceLogEntity {

    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    @TableField("user_id")
    private Long userId;

    @TableField("change_type")
    private String changeType;

    @TableField("change_amount")
    private BigDecimal changeAmount;

    @TableField("balance_before")
    private BigDecimal balanceBefore;

    @TableField("balance_after")
    private BigDecimal balanceAfter;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_id")
    private Long bizId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
