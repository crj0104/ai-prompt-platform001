package com.course.promptplatform.service;

import com.course.promptplatform.model.ApiRequests.LoginRequest;
import com.course.promptplatform.model.ApiRequests.PurchaseRequest;
import com.course.promptplatform.model.ApiRequests.RegisterRequest;
import com.course.promptplatform.model.ApiRequests.ReviewRequest;
import com.course.promptplatform.model.ApiRequests.SearchRequest;
import com.course.promptplatform.model.PortalViewModels.DailyTrendView;
import com.course.promptplatform.model.PortalViewModels.TemplateCardView;
import com.course.promptplatform.model.PortalViewModels.TemplateDetailView;
import com.course.promptplatform.model.PortalViewModels.TemplateRankView;
import com.course.promptplatform.model.PortalViewModels.UserProfileView;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortalApplicationService implements PortalService {

    private static final Long DEFAULT_USER_ID = 1L;

    private final TemplateDomainService templateDomainService;
    private final TemplateAnalyticsService templateAnalyticsService;
    private final TradeService tradeService;
    private final UserCenterService userCenterService;
    private final DashboardService dashboardService;

    @Override
    public List<TemplateCardView> listTemplates() {
        return templateAnalyticsService.listHotTemplates(7, 9);
    }

    @Override
    public Map<String, Object> searchTemplates(SearchRequest request, long current, long size) {
        return templateDomainService.searchTemplates(request, current, size);
    }

    @Override
    public TemplateDetailView getTemplateDetail(Long templateId) {
        return templateDomainService.getTemplateDetail(templateId);
    }

    @Override
    public UserProfileView getProfile() {
        return userCenterService.getProfile(DEFAULT_USER_ID);
    }

    @Override
    public Map<String, Object> creatorDashboard() {
        return dashboardService.creatorDashboard(DEFAULT_USER_ID);
    }

    @Override
    public Map<String, Object> adminDashboard() {
        return dashboardService.adminDashboard();
    }

    @Override
    public Map<String, Object> register(RegisterRequest request) {
        return userCenterService.register(request);
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        return userCenterService.login(request);
    }

    @Override
    public Map<String, Object> toggleFavorite(Long templateId) {
        return tradeService.toggleFavorite(DEFAULT_USER_ID, templateId);
    }

    @Override
    public Map<String, Object> removeFavorite(Long templateId) {
        return tradeService.removeFavorite(DEFAULT_USER_ID, templateId);
    }

    @Override
    public Map<String, Object> purchaseTemplate(PurchaseRequest request) {
        return tradeService.purchaseTemplate(DEFAULT_USER_ID, request);
    }

    @Override
    public Map<String, Object> useTemplate(Long templateId, String inputSummary) {
        return tradeService.useTemplate(DEFAULT_USER_ID, templateId, inputSummary);
    }

    @Override
    public Map<String, Object> submitReview(ReviewRequest request) {
        return tradeService.submitReview(DEFAULT_USER_ID, request);
    }

    @Override
    public List<TemplateCardView> recommendTemplates() {
        return templateDomainService.recommendTemplates();
    }

    @Override
    public List<String> allTags() {
        return templateDomainService.allTags();
    }

    @Override
    public List<DailyTrendView> queryUsageTrend(Long templateId, int days) {
        return templateAnalyticsService.queryUsageTrend(templateId, days);
    }

    @Override
    public List<TemplateRankView> queryHotRanking(int days, int limit) {
        return templateAnalyticsService.queryHotRanking(days, limit);
    }
}
