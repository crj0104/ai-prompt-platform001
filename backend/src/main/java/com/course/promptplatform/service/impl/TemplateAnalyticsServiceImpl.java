package com.course.promptplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.course.promptplatform.entity.TagEntity;
import com.course.promptplatform.entity.TemplateTagRelEntity;
import com.course.promptplatform.mapper.PromptTemplateMapper;
import com.course.promptplatform.mapper.TagMapper;
import com.course.promptplatform.mapper.TemplateTagRelMapper;
import com.course.promptplatform.mapper.TemplateUseLogMapper;
import com.course.promptplatform.model.PortalViewModels.DailyTrendView;
import com.course.promptplatform.model.PortalViewModels.TemplateCardView;
import com.course.promptplatform.model.PortalViewModels.TemplateRankView;
import com.course.promptplatform.model.query.TemplateQueryRow;
import com.course.promptplatform.model.query.UsageTrendRow;
import com.course.promptplatform.service.TemplateAnalyticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TemplateAnalyticsServiceImpl implements TemplateAnalyticsService {

    private final PromptTemplateMapper promptTemplateMapper;
    private final TemplateUseLogMapper templateUseLogMapper;
    private final TemplateTagRelMapper templateTagRelMapper;
    private final TagMapper tagMapper;

    @Override
    public List<DailyTrendView> queryUsageTrend(Long templateId, int days) {
        int safeDays = Math.max(1, days);
        LocalDate startDate = LocalDate.now().minusDays(safeDays - 1L);
        List<UsageTrendRow> rows = templateUseLogMapper.selectDailyUsageTrend(templateId, startDate.atStartOfDay());
        Map<LocalDate, Integer> trendMap = rows.stream()
                .collect(Collectors.toMap(UsageTrendRow::getStatDate, UsageTrendRow::getUsageCount));
        return IntStream.range(0, safeDays)
                .mapToObj(index -> {
                    LocalDate date = startDate.plusDays(index);
                    return new DailyTrendView(date, trendMap.getOrDefault(date, 0));
                })
                .toList();
    }

    @Override
    public List<TemplateRankView> queryHotRanking(int days, int limit) {
        int safeDays = Math.max(1, days);
        int safeLimit = Math.max(1, limit);
        List<TemplateQueryRow> rows = promptTemplateMapper.selectHotRanking(LocalDateTime.now().minusDays(safeDays), safeLimit);
        Map<Long, List<String>> tagMap = resolveTags(rows.stream().map(TemplateQueryRow::getTemplateId).toList());
        return IntStream.range(0, rows.size())
                .mapToObj(index -> {
                    TemplateQueryRow row = rows.get(index);
                    return TemplateRankView.builder()
                            .rankNo(index + 1)
                            .templateId(row.getTemplateId())
                            .title(row.getTitle())
                            .scene(row.getSceneDesc())
                            .price(row.getPrice())
                            .free(isFree(row.getPriceType(), row.getPrice()))
                            .useCount(row.getUseCount())
                            .favoriteCount(row.getFavoriteCount())
                            .avgScore(row.getAvgScore())
                            .recentUseCount(row.getRecentUseCount() == null ? 0 : row.getRecentUseCount())
                            .hotScore(row.getHotScore() == null ? BigDecimal.ZERO : row.getHotScore())
                            .tags(tagMap.getOrDefault(row.getTemplateId(), List.of()))
                            .build();
                })
                .toList();
    }

    @Override
    public List<TemplateCardView> listHotTemplates(int days, int limit) {
        return queryHotRanking(days, limit).stream()
                .map(item -> TemplateCardView.builder()
                        .id(item.getTemplateId())
                        .title(item.getTitle())
                        .scene(item.getScene())
                        .summary("综合热度分 " + item.getHotScore() + "，近周期使用 " + item.getRecentUseCount() + " 次")
                        .price(item.getPrice())
                        .free(item.isFree())
                        .status("已上架")
                        .useCount(item.getUseCount())
                        .favoriteCount(item.getFavoriteCount())
                        .avgScore(item.getAvgScore())
                        .tags(item.getTags())
                        .build())
                .toList();
    }

    private Map<Long, List<String>> resolveTags(List<Long> templateIds) {
        if (templateIds.isEmpty()) {
            return Map.of();
        }
        List<TemplateTagRelEntity> relations = templateTagRelMapper.selectList(
                new LambdaQueryWrapper<TemplateTagRelEntity>().in(TemplateTagRelEntity::getTemplateId, templateIds));
        if (relations.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> tagMap = tagMapper.selectBatchIds(relations.stream().map(TemplateTagRelEntity::getTagId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(TagEntity::getTagId, TagEntity::getTagName, (left, right) -> left));
        return relations.stream().collect(Collectors.groupingBy(
                TemplateTagRelEntity::getTemplateId,
                Collectors.mapping(rel -> tagMap.get(rel.getTagId()), Collectors.toList())));
    }

    private boolean isFree(String priceType, BigDecimal price) {
        return "FREE".equalsIgnoreCase(priceType) || price.compareTo(BigDecimal.ZERO) == 0;
    }
}
