package com.course.promptplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("template_favorite")
public class TemplateFavoriteEntity {

    @TableId(value = "favorite_id", type = IdType.AUTO)
    private Long favoriteId;

    @TableField("user_id")
    private Long userId;

    @TableField("template_id")
    private Long templateId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
