package com.course.promptplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("tag")
public class TagEntity {

    @TableId(value = "tag_id", type = IdType.AUTO)
    private Long tagId;

    @TableField("tag_name")
    private String tagName;

    @TableField("parent_tag_id")
    private Long parentTagId;

    @TableField("tag_level")
    private Integer tagLevel;

    @TableField("tag_path")
    private String tagPath;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
