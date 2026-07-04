package com.course.promptplatform.service;

import com.course.promptplatform.model.ApiRequests.PurchaseRequest;
import com.course.promptplatform.model.ApiRequests.ReviewRequest;
import com.course.promptplatform.model.PortalViewModels.BalanceLogView;
import com.course.promptplatform.model.PortalViewModels.OrderView;
import com.course.promptplatform.model.PortalViewModels.UsageLogView;
import java.util.List;
import java.util.Map;

public interface TradeService {

    Map<String, Object> toggleFavorite(Long userId, Long templateId);

    Map<String, Object> removeFavorite(Long userId, Long templateId);

    Map<String, Object> purchaseTemplate(Long userId, PurchaseRequest request);

    Map<String, Object> useTemplate(Long userId, Long templateId, String inputSummary);

    Map<String, Object> submitReview(Long userId, ReviewRequest request);

    List<OrderView> listOrders(Long userId);

    List<UsageLogView> listUsageLogs(Long userId);

    List<BalanceLogView> listBalanceLogs(Long userId);

    Map<String, Object> setTemplateFree(Long templateId);
}
