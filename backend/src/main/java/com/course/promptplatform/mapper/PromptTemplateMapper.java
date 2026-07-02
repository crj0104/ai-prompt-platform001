package com.course.promptplatform.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.course.promptplatform.entity.PromptTemplateEntity;
import com.course.promptplatform.model.ApiRequests.SearchRequest;
import com.course.promptplatform.model.query.TemplateQueryRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 模板 Mapper，直接复用 MyBatis-Plus 的通用 CRUD 与分页能力。
 */
public interface PromptTemplateMapper extends BaseMapper<PromptTemplateEntity> {

    IPage<TemplateQueryRow> searchTemplatePage(IPage<TemplateQueryRow> page, @Param("req") SearchRequest request);

    List<TemplateQueryRow> selectHotRanking(@Param("startTime") LocalDateTime startTime, @Param("limit") Integer limit);
}
