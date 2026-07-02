package com.course.promptplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("template_order")
public class TemplateOrderEntity {

    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("template_id")
    private Long templateId;

    @TableField("origin_amount")
    private BigDecimal originAmount;

    @TableField("pay_amount")
    private BigDecimal payAmount;

    @TableField("pay_status")
    private String payStatus;

    @TableField("order_status")
    private String orderStatus;

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
