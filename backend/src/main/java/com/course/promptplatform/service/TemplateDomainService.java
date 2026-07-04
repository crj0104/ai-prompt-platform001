package com.course.promptplatform.service;

import com.course.promptplatform.model.ApiRequests.PublishTemplateRequest;
import com.course.promptplatform.model.ApiRequests.SearchRequest;
import com.course.promptplatform.model.ApiRequests.UpdateTemplateRequest;
import com.course.promptplatform.model.PortalViewModels.TemplateCardView;
import com.course.promptplatform.model.PortalViewModels.TemplateDetailView;
import java.util.List;
import java.util.Map;

public interface TemplateDomainService {

    List<TemplateCardView> listTemplates();

    Map<String, Object> searchTemplates(SearchRequest request, long current, long size);

    TemplateDetailView getTemplateDetail(Long templateId);

    List<TemplateCardView> recommendTemplates();

    List<String> allTags();

    List<TemplateCardView> publishedTemplates(Long userId);

    List<TemplateCardView> favoriteTemplates(Long userId);

    Map<String, Object> publishTemplate(Long userId, PublishTemplateRequest request);

    Map<String, Object> updateTemplate(Long userId, Long templateId, UpdateTemplateRequest request);

    Map<String, Object> deleteTemplate(Long userId, Long templateId);
}
