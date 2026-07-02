package com.course.promptplatform.service;

import com.course.promptplatform.model.PortalViewModels.DailyTrendView;
import com.course.promptplatform.model.PortalViewModels.TemplateCardView;
import com.course.promptplatform.model.PortalViewModels.TemplateRankView;
import java.util.List;

public interface TemplateAnalyticsService {

    List<DailyTrendView> queryUsageTrend(Long templateId, int days);

    List<TemplateRankView> queryHotRanking(int days, int limit);

    List<TemplateCardView> listHotTemplates(int days, int limit);
}
