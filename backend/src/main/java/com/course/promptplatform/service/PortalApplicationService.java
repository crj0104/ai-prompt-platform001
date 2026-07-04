package com.course.promptplatform.service;

import com.course.promptplatform.model.ApiRequests.LoginRequest;
import com.course.promptplatform.model.ApiRequests.PublishTemplateRequest;
import com.course.promptplatform.model.ApiRequests.PurchaseRequest;
import com.course.promptplatform.model.ApiRequests.RegisterRequest;
import com.course.promptplatform.model.ApiRequests.ReviewRequest;
import com.course.promptplatform.model.ApiRequests.SearchRequest;
import com.course.promptplatform.model.ApiRequests.UpdateTemplateRequest;
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
    public UserProfileView getProfile(Long userId) {
        return userCenterService.getProfile(userId);
    }

    @Override
    public Map<String, Object> upgradeToCreator(Long userId) {
        return userCenterService.upgradeToCreator(userId);
    }

    @Override
    public Map<String, Object> creatorDashboard(Long userId) {
        return dashboardService.creatorDashboard(userId);
    }

    @Override
    public Map<String, Object> adminDashboard() {
        return dashboardService.adminDashboard();
    }

    @Override
    public Map<String, Object> setTemplateFree(Long templateId) {
        return tradeService.setTemplateFree(templateId);
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
    public Map<String, Object> toggleFavorite(Long userId, Long templateId) {
        return tradeService.toggleFavorite(userId, templateId);
    }

    @Override
    public Map<String, Object> removeFavorite(Long userId, Long templateId) {
        return tradeService.removeFavorite(userId, templateId);
    }

    @Override
    public Map<String, Object> purchaseTemplate(Long userId, PurchaseRequest request) {
        return tradeService.purchaseTemplate(userId, request);
    }

    @Override
    public Map<String, Object> publishTemplate(Long userId, PublishTemplateRequest request) {
        return templateDomainService.publishTemplate(userId, request);
    }

    @Override
    public Map<String, Object> updateTemplate(Long userId, Long templateId, UpdateTemplateRequest request) {
        return templateDomainService.updateTemplate(userId, templateId, request);
    }

    @Override
    public Map<String, Object> deleteTemplate(Long userId, Long templateId) {
        return templateDomainService.deleteTemplate(userId, templateId);
    }

    @Override
    public Map<String, Object> useTemplate(Long userId, Long templateId, String inputSummary) {
        return tradeService.useTemplate(userId, templateId, inputSummary);
    }

    @Override
    public Map<String, Object> submitReview(Long userId, ReviewRequest request) {
        return tradeService.submitReview(userId, request);
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
