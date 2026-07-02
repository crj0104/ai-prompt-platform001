package com.course.promptplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("template_tag_rel")
public class TemplateTagRelEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("template_id")
    private Long templateId;

    @TableField("tag_id")
    private Long tagId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
