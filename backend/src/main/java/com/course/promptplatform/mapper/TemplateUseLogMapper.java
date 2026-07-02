package com.course.promptplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.course.promptplatform.entity.TemplateUseLogEntity;
import com.course.promptplatform.model.query.UsageTrendRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TemplateUseLogMapper extends BaseMapper<TemplateUseLogEntity> {

    List<UsageTrendRow> selectDailyUsageTrend(@Param("templateId") Long templateId, @Param("startTime") LocalDateTime startTime);
}
