package com.course.promptplatform.model.query;

import java.time.LocalDate;
import lombok.Data;

/**
 * 使用趋势聚合结果。
 */
@Data
public class UsageTrendRow {

    private LocalDate statDate;
    private Integer usageCount;
}
