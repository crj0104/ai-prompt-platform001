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

/**
 * 门户业务接口，控制器仅依赖抽象，便于后续新增实现类时保持对扩展开放、对修改关闭。
 */
public interface PortalService {

    List<TemplateCardView> listTemplates();

    Map<String, Object> searchTemplates(SearchRequest request, long current, long size);

    TemplateDetailView getTemplateDetail(Long templateId);

    UserProfileView getProfile(Long userId);

    Map<String, Object> upgradeToCreator(Long userId);

    Map<String, Object> creatorDashboard(Long userId);

    Map<String, Object> adminDashboard();

    Map<String, Object> setTemplateFree(Long templateId);

    Map<String, Object> register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);

    Map<String, Object> toggleFavorite(Long userId, Long templateId);

    Map<String, Object> removeFavorite(Long userId, Long templateId);

    Map<String, Object> purchaseTemplate(Long userId, PurchaseRequest request);

    Map<String, Object> publishTemplate(Long userId, PublishTemplateRequest request);

    Map<String, Object> updateTemplate(Long userId, Long templateId, UpdateTemplateRequest request);

    Map<String, Object> deleteTemplate(Long userId, Long templateId);

    Map<String, Object> useTemplate(Long userId, Long templateId, String inputSummary);

    Map<String, Object> submitReview(Long userId, ReviewRequest request);

    List<TemplateCardView> recommendTemplates();

    List<String> allTags();

    List<DailyTrendView> queryUsageTrend(Long templateId, int days);

    List<TemplateRankView> queryHotRanking(int days, int limit);
}
