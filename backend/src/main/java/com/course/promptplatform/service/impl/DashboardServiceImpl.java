package com.course.promptplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.course.promptplatform.entity.CampaignTemplateEntity;
import com.course.promptplatform.entity.FreeCampaignEntity;
import com.course.promptplatform.entity.PromptTemplateEntity;
import com.course.promptplatform.entity.SysUserEntity;
import com.course.promptplatform.entity.TemplateOrderEntity;
import com.course.promptplatform.entity.TemplateUseLogEntity;
import com.course.promptplatform.mapper.CampaignTemplateMapper;
import com.course.promptplatform.mapper.FreeCampaignMapper;
import com.course.promptplatform.mapper.PromptTemplateMapper;
import com.course.promptplatform.mapper.SysUserMapper;
import com.course.promptplatform.mapper.TemplateOrderMapper;
import com.course.promptplatform.mapper.TemplateUseLogMapper;
import com.course.promptplatform.model.PortalViewModels.CampaignView;
import com.course.promptplatform.model.PortalViewModels.DailyTrendView;
import com.course.promptplatform.model.PortalViewModels.MonthlyIncomeView;
import com.course.promptplatform.model.PortalViewModels.StatCardView;
import com.course.promptplatform.model.PortalViewModels.TemplateCardView;
import com.course.promptplatform.model.PortalViewModels.TemplateIncomeView;
import com.course.promptplatform.service.DashboardService;
import com.course.promptplatform.service.TemplateAnalyticsService;
import com.course.promptplatform.service.TemplateDomainService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TemplateDomainService templateDomainService;
    private final TemplateAnalyticsService templateAnalyticsService;
    private final PromptTemplateMapper promptTemplateMapper;
    private final SysUserMapper sysUserMapper;
    private final TemplateOrderMapper templateOrderMapper;
    private final TemplateUseLogMapper templateUseLogMapper;
    private final FreeCampaignMapper freeCampaignMapper;
    private final CampaignTemplateMapper campaignTemplateMapper;

    @Override
    public Map<String, Object> creatorDashboard(Long userId) {
        List<TemplateCardView> templateCards = templateDomainService.publishedTemplates(userId);
        List<Long> templateIds = templateCards.stream().map(TemplateCardView::getId).toList();
        SysUserEntity user = sysUserMapper.selectById(userId);
        String creatorLevel = user != null && user.getCreatorLevel() != null ? user.getCreatorLevel() : "-";
        BigDecimal monthIncome = calcMonthIncome(templateIds);
        return Map.of(
                "statCards", List.of(
                        new StatCardView("模板总数", String.valueOf(templateCards.size()), ""),
                        new StatCardView("累计使用", String.valueOf(templateCards.stream().mapToInt(TemplateCardView::getUseCount).sum()), ""),
                        new StatCardView("本月收入", "￥" + monthIncome.toString(), ""),
                        new StatCardView("创作者等级", creatorLevel, "")),
                "trend", buildUsageTrend(templateIds, 30),
                "income", buildMonthlyIncome(templateIds, 6),
                "templateIncome", buildTemplateIncome(templateCards),
                "templates", templateCards);
    }

    private BigDecimal calcMonthIncome(List<Long> templateIds) {
        if (templateIds.isEmpty()) return BigDecimal.ZERO;
        YearMonth now = YearMonth.now();
        LocalDate start = now.atDay(1);
        LocalDate end = now.atEndOfMonth();
        return templateOrderMapper.selectList(
                new LambdaQueryWrapper<TemplateOrderEntity>()
                        .in(TemplateOrderEntity::getTemplateId, templateIds)
                        .eq(TemplateOrderEntity::getPayStatus, "PAID")
                        .between(TemplateOrderEntity::getPayTime, start.atStartOfDay(), end.plusDays(1).atStartOfDay())
        ).stream()
                .map(TemplateOrderEntity::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Map<String, Object> adminDashboard() {
        List<TemplateCardView> templates = templateDomainService.listTemplates();
        List<CampaignView> campaigns = buildCampaignViews();
        long totalUsers = sysUserMapper.selectCount(new LambdaQueryWrapper<>());
        long totalTemplates = promptTemplateMapper.selectCount(new LambdaQueryWrapper<PromptTemplateEntity>());
        BigDecimal totalRevenue = templateOrderMapper.selectList(
                new LambdaQueryWrapper<TemplateOrderEntity>().eq(TemplateOrderEntity::getPayStatus, "PAID"))
                .stream().map(TemplateOrderEntity::getPayAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long todayActive = templateUseLogMapper.selectCount(
                new LambdaQueryWrapper<TemplateUseLogEntity>().ge(TemplateUseLogEntity::getUsedAt, LocalDate.now().atStartOfDay()));
        return Map.of(
                "statCards", List.of(
                        new StatCardView("总用户数", String.valueOf(totalUsers), ""),
                        new StatCardView("总模板数", String.valueOf(totalTemplates), ""),
                        new StatCardView("总交易额", "￥" + totalRevenue.toString(), ""),
                        new StatCardView("日活用户数", String.valueOf(todayActive), "")),
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

    private List<MonthlyIncomeView> buildMonthlyIncome(List<Long> templateIds, int months) {
        if (templateIds.isEmpty()) {
            return List.of();
        }
        YearMonth current = YearMonth.now();
        LocalDate start = current.minusMonths(months - 1).atDay(1);
        Map<String, BigDecimal> incomeMap = new LinkedHashMap<>();
        templateOrderMapper.selectList(new LambdaQueryWrapper<TemplateOrderEntity>()
                        .in(TemplateOrderEntity::getTemplateId, templateIds)
                        .eq(TemplateOrderEntity::getOrderStatus, "SUCCESS")
                        .eq(TemplateOrderEntity::getPayStatus, "PAID")
                        .gt(TemplateOrderEntity::getPayAmount, BigDecimal.ZERO)
                        .ge(TemplateOrderEntity::getPayTime, start.atStartOfDay())
                        .orderByAsc(TemplateOrderEntity::getPayTime))
                .forEach(order -> {
                    String month = YearMonth.from(order.getPayTime()).toString();
                    incomeMap.merge(month, order.getPayAmount(), BigDecimal::add);
                });
        return incomeMap.entrySet().stream()
                .map(entry -> new MonthlyIncomeView(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<TemplateIncomeView> buildTemplateIncome(List<TemplateCardView> templates) {
        if (templates.isEmpty()) {
            return List.of();
        }
        Map<Long, TemplateCardView> templateMap = templates.stream()
                .collect(Collectors.toMap(TemplateCardView::getId, item -> item));
        Map<Long, List<TemplateOrderEntity>> ordersByTemplate = templateOrderMapper.selectList(new LambdaQueryWrapper<TemplateOrderEntity>()
                        .in(TemplateOrderEntity::getTemplateId, templateMap.keySet())
                        .eq(TemplateOrderEntity::getOrderStatus, "SUCCESS")
                        .eq(TemplateOrderEntity::getPayStatus, "PAID"))
                .stream()
                .collect(Collectors.groupingBy(TemplateOrderEntity::getTemplateId));
        return templates.stream()
                .map(template -> {
                    List<TemplateOrderEntity> orders = ordersByTemplate.getOrDefault(template.getId(), List.of());
                    BigDecimal income = orders.stream()
                            .map(TemplateOrderEntity::getPayAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new TemplateIncomeView(template.getId(), template.getTitle(), income, orders.size());
                })
                .sorted((left, right) -> right.getIncome().compareTo(left.getIncome()))
                .toList();
    }

    private List<DailyTrendView> buildUsageTrend(List<Long> templateIds, int days) {
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        Map<LocalDate, Integer> trendMap = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            trendMap.put(start.plusDays(i), 0);
        }
        if (!templateIds.isEmpty()) {
            templateUseLogMapper.selectList(new LambdaQueryWrapper<TemplateUseLogEntity>()
                            .in(TemplateUseLogEntity::getTemplateId, templateIds)
                            .ge(TemplateUseLogEntity::getUsedAt, start.atStartOfDay()))
                    .forEach(log -> {
                        LocalDate date = log.getUsedAt().toLocalDate();
                        trendMap.computeIfPresent(date, (key, value) -> value + 1);
                    });
        }
        return trendMap.entrySet().stream()
                .map(entry -> new DailyTrendView(entry.getKey(), entry.getValue()))
                .toList();
    }

}
