package com.course.promptplatform.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_user")
public class SysUserEntity {

    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    private String username;

    @TableField("password_hash")
    private String passwordHash;

    private String phone;

    private String email;

    @TableField("creator_score")
    private Integer creatorScore;

    @TableField(value = "creator_level", insertStrategy = FieldStrategy.NEVER)
    private String creatorLevel;

    private BigDecimal balance;

    @TableField("user_status")
    private String userStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
