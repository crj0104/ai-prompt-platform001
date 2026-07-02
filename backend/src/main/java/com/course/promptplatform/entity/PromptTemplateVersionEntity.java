package com.course.promptplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("prompt_template_version")
public class PromptTemplateVersionEntity {

    @TableId(value = "version_id", type = IdType.AUTO)
    private Long versionId;

    @TableField("template_id")
    private Long templateId;

    @TableField("version_no")
    private String versionNo;

    @TableField("prompt_content")
    private String promptContent;

    @TableField("change_note")
    private String changeNote;

    @TableField("editor_user_id")
    private Long editorUserId;

    @TableField("source_version_id")
    private Long sourceVersionId;

    @TableField("rollback_from_version_id")
    private Long rollbackFromVersionId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
