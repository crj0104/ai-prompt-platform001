package com.course.promptplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.course.promptplatform.entity.CampaignTemplateEntity;
import com.course.promptplatform.entity.FreeCampaignEntity;
import com.course.promptplatform.entity.PromptTemplateEntity;
import com.course.promptplatform.mapper.CampaignTemplateMapper;
import com.course.promptplatform.mapper.FreeCampaignMapper;
import com.course.promptplatform.mapper.PromptTemplateMapper;
import com.course.promptplatform.model.PortalViewModels.CampaignView;
import com.course.promptplatform.model.PortalViewModels.MonthlyIncomeView;
import com.course.promptplatform.model.PortalViewModels.StatCardView;
import com.course.promptplatform.model.PortalViewModels.TemplateCardView;
import com.course.promptplatform.model.PortalViewModels.VersionView;
import com.course.promptplatform.service.DashboardService;
import com.course.promptplatform.service.TemplateAnalyticsService;
import com.course.promptplatform.service.TemplateDomainService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TemplateDomainService templateDomainService;
    private final TemplateAnalyticsService templateAnalyticsService;
    private final PromptTemplateMapper promptTemplateMapper;
    private final FreeCampaignMapper freeCampaignMapper;
    private final CampaignTemplateMapper campaignTemplateMapper;

    @Override
    public Map<String, Object> creatorDashboard(Long userId) {
        List<TemplateCardView> templateCards = templateDomainService.publishedTemplates(userId);
        Long trendTemplateId = templateCards.isEmpty() ? 101L : templateCards.get(0).getId();
        return Map.of(
                "statCards", List.of(
                        new StatCardView("模板总数", String.valueOf(templateCards.size()), "+2 本周"),
                        new StatCardView("累计使用", String.valueOf(templateCards.stream().mapToInt(TemplateCardView::getUseCount).sum()), "+18%"),
                        new StatCardView("本月收入", "¥1280.00", "+12%"),
                        new StatCardView("创作者等级", "A级创作者", "自动更新")),
                "trend", templateAnalyticsService.queryUsageTrend(trendTemplateId, 30),
                "income", buildMonthlyIncome(),
                "versions", buildVersionSummary());
    }

    @Override
    public Map<String, Object> adminDashboard() {
        List<TemplateCardView> templates = templateDomainService.listTemplates();
        List<CampaignView> campaigns = buildCampaignViews();
        return Map.of(
                "statCards", List.of(
                        new StatCardView("总用户数", "1286", "+8%"),
                        new StatCardView("总模板数", String.valueOf(promptTemplateMapper.selectCount(new LambdaQueryWrapper<PromptTemplateEntity>())), "+3"),
                        new StatCardView("总交易额", "¥52860.00", "+16%"),
                        new StatCardView("日活用户数", "387", "+5%")),
                "campaigns", campaigns,
                "templates", templateAnalyticsService.listHotTemplates(7, Math.max(templates.size(), 6)));
    }

    private List<CampaignView> buildCampaignViews() {
        List<FreeCampaignEntity> campaigns = freeCampaignMapper.selectList(new LambdaQueryWrapper<FreeCampaignEntity>().orderByDesc(FreeCampaignEntity::getCreatedAt));
        Map<Long, PromptTemplateEntity> templateMap = promptTemplateMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .collect(java.util.stream.Collectors.toMap(PromptTemplateEntity::getTemplateId, item -> item));
        return campaignTemplateMapper.selectList(new LambdaQueryWrapper<CampaignTemplateEntity>()).stream()
                .map(item -> {
                    FreeCampaignEntity campaign = campaigns.stream()
                            .filter(c -> c.getCampaignId().equals(item.getCampaignId()))
                            .findFirst()
                            .orElse(null);
                    PromptTemplateEntity template = templateMap.get(item.getTemplateId());
                    return CampaignView.builder()
                            .id(item.getCampaignTemplateId())
                            .name(campaign == null ? "未知活动" : campaign.getCampaignName())
                            .templateTitle(template == null ? "未知模板" : template.getTitle())
                            .totalQuota(item.getTotalQuota())
                            .remainingQuota(item.getRemainingQuota())
                            .startTime(campaign == null ? null : campaign.getStartTime())
                            .endTime(campaign == null ? null : campaign.getEndTime())
                            .status(campaign == null ? "UNKNOWN" : campaign.getCampaignStatus())
                            .build();
                })
                .toList();
    }

    private List<MonthlyIncomeView> buildMonthlyIncome() {
        return List.of(
                new MonthlyIncomeView("2026-04", new BigDecimal("820.00")),
                new MonthlyIncomeView("2026-05", new BigDecimal("1080.00")),
                new MonthlyIncomeView("2026-06", new BigDecimal("1280.00")));
    }

    private List<VersionView> buildVersionSummary() {
        return List.of(
                new VersionView(1002L, "v1.0.0", "初始化模板", "请从正确性、性能、可维护性和安全性四个方面评审以下代码。", "creator_one", java.time.LocalDateTime.now().minusDays(25)),
                new VersionView(1003L, "v1.1.0", "优化结构与输出格式", "请从正确性、性能、可维护性和安全性四个方面评审以下代码，并以问题列表和优化建议两部分输出。", "creator_one", java.time.LocalDateTime.now().minusDays(12)));
    }
}
