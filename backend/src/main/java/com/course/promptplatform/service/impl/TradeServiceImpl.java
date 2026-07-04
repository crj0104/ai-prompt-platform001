package com.course.promptplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.course.promptplatform.entity.CampaignClaimEntity;
import com.course.promptplatform.entity.CampaignTemplateEntity;
import com.course.promptplatform.entity.FreeCampaignEntity;
import com.course.promptplatform.entity.PromptTemplateEntity;
import com.course.promptplatform.entity.PromptTemplateVersionEntity;
import com.course.promptplatform.entity.SysUserEntity;
import com.course.promptplatform.entity.TemplateFavoriteEntity;
import com.course.promptplatform.entity.TemplateOrderEntity;
import com.course.promptplatform.entity.TemplateReviewEntity;
import com.course.promptplatform.entity.TemplateUseLogEntity;
import com.course.promptplatform.entity.UserBalanceLogEntity;
import com.course.promptplatform.mapper.CampaignClaimMapper;
import com.course.promptplatform.mapper.CampaignTemplateMapper;
import com.course.promptplatform.mapper.FreeCampaignMapper;
import com.course.promptplatform.mapper.PromptTemplateMapper;
import com.course.promptplatform.mapper.PromptTemplateVersionMapper;
import com.course.promptplatform.mapper.SysUserMapper;
import com.course.promptplatform.mapper.TemplateFavoriteMapper;
import com.course.promptplatform.mapper.TemplateOrderMapper;
import com.course.promptplatform.mapper.TemplateReviewMapper;
import com.course.promptplatform.mapper.TemplateUseLogMapper;
import com.course.promptplatform.mapper.UserBalanceLogMapper;
import com.course.promptplatform.model.ApiRequests.PurchaseRequest;
import com.course.promptplatform.model.ApiRequests.ReviewRequest;
import com.course.promptplatform.model.PortalViewModels.BalanceLogView;
import com.course.promptplatform.model.PortalViewModels.OrderView;
import com.course.promptplatform.model.PortalViewModels.UsageLogView;
import com.course.promptplatform.service.TradeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeService {

    private final PromptTemplateMapper promptTemplateMapper;
    private final TemplateFavoriteMapper templateFavoriteMapper;
    private final TemplateOrderMapper templateOrderMapper;
    private final TemplateUseLogMapper templateUseLogMapper;
    private final TemplateReviewMapper templateReviewMapper;
    private final UserBalanceLogMapper userBalanceLogMapper;
    private final PromptTemplateVersionMapper versionMapper;
    private final SysUserMapper sysUserMapper;
    private final FreeCampaignMapper freeCampaignMapper;
    private final CampaignTemplateMapper campaignTemplateMapper;
    private final CampaignClaimMapper campaignClaimMapper;

    @Override
    @Transactional
    public Map<String, Object> toggleFavorite(Long userId, Long templateId) {
        PromptTemplateEntity template = promptTemplateMapper.selectById(templateId);
        if (template == null) {
            return Map.of("success", false, "message", "Template not found");
        }
        TemplateFavoriteEntity favorite = findFavorite(userId, templateId);
        if (favorite != null) {
            return removeFavorite(userId, templateId);
        }

        TemplateFavoriteEntity entity = new TemplateFavoriteEntity();
        entity.setUserId(userId);
        entity.setTemplateId(templateId);
        entity.setCreatedAt(LocalDateTime.now());
        templateFavoriteMapper.insert(entity);
        incrementTemplateCounter(templateId, "favorite_count", 1);

        PromptTemplateEntity updatedTemplate = promptTemplateMapper.selectById(templateId);
        return Map.of("success", true, "favoriteCount", updatedTemplate.getFavoriteCount(), "message", "Favorite added");
    }

    @Override
    @Transactional
    public Map<String, Object> removeFavorite(Long userId, Long templateId) {
        TemplateFavoriteEntity favorite = findFavorite(userId, templateId);
        if (favorite == null) {
            PromptTemplateEntity template = promptTemplateMapper.selectById(templateId);
            int favoriteCount = template == null ? 0 : template.getFavoriteCount();
            return Map.of("success", true, "favoriteCount", favoriteCount, "message", "Favorite already removed");
        }

        templateFavoriteMapper.deleteById(favorite.getFavoriteId());
        promptTemplateMapper.update(null, new LambdaUpdateWrapper<PromptTemplateEntity>()
                .eq(PromptTemplateEntity::getTemplateId, templateId)
                .gt(PromptTemplateEntity::getFavoriteCount, 0)
                .setSql("favorite_count = favorite_count - 1"));

        PromptTemplateEntity template = promptTemplateMapper.selectById(templateId);
        return Map.of("success", true, "favoriteCount", template == null ? 0 : template.getFavoriteCount(), "message", "Favorite removed");
    }

    @Override
    @Transactional
    public Map<String, Object> purchaseTemplate(Long userId, PurchaseRequest request) {
        PromptTemplateEntity template = promptTemplateMapper.selectById(request.getTemplateId());
        SysUserEntity user = sysUserMapper.selectById(userId);
        if (template == null) {
            return Map.of("success", false, "message", "Template not found");
        }
        if (user == null) {
            return Map.of("success", false, "message", "User not found");
        }
        if (!"ON_SHELF".equalsIgnoreCase(template.getShelfStatus())) {
            return Map.of("success", false, "message", "Template is not on shelf");
        }

        TemplateOrderEntity existingOrder = findSuccessfulOrder(userId, template.getTemplateId());
        if (existingOrder != null) {
            return Map.of(
                    "success", true,
                    "message", "Template already purchased",
                    "order", toOrderView(existingOrder, template),
                    "balance", user.getBalance());
        }

        CampaignTemplateEntity activeCampaignTemplate = findActiveCampaignTemplate(template.getTemplateId());
        if (activeCampaignTemplate != null) {
            return claimCampaignTemplate(user, template, activeCampaignTemplate);
        }

        boolean free = isFree(template);
        if (!free && user.getBalance().compareTo(template.getPrice()) < 0) {
            return Map.of("success", false, "message", "Insufficient balance");
        }

        BigDecimal balanceBefore = user.getBalance();
        BigDecimal payAmount = free ? BigDecimal.ZERO : template.getPrice();
        user.setBalance(balanceBefore.subtract(payAmount));
        sysUserMapper.updateById(user);

        TemplateOrderEntity order = new TemplateOrderEntity();
        order.setOrderNo("ORD" + System.currentTimeMillis());
        order.setUserId(userId);
        order.setTemplateId(template.getTemplateId());
        order.setOriginAmount(template.getPrice());
        order.setPayAmount(payAmount);
        order.setPayStatus(free ? "FREE_CLAIMED" : "PAID");
        order.setOrderStatus("SUCCESS");
        order.setPayTime(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        templateOrderMapper.insert(order);

        UserBalanceLogEntity log = new UserBalanceLogEntity();
        log.setUserId(userId);
        log.setChangeType(free ? "FREE_CLAIM" : "PAYMENT");
        log.setChangeAmount(payAmount.negate());
        log.setBalanceBefore(balanceBefore);
        log.setBalanceAfter(user.getBalance());
        log.setBizType("TEMPLATE_PURCHASE");
        log.setBizId(order.getOrderId());
        log.setCreatedAt(LocalDateTime.now());
        userBalanceLogMapper.insert(log);

        return Map.of("success", true, "message", "Order created", "order", toOrderView(order, template), "balance", user.getBalance());
    }

    @Override
    @Transactional
    public Map<String, Object> useTemplate(Long userId, Long templateId, String inputSummary) {
        PromptTemplateEntity template = promptTemplateMapper.selectById(templateId);
        if (template == null) {
            return Map.of("success", false, "message", "Template not found");
        }
        if (!"ON_SHELF".equalsIgnoreCase(template.getShelfStatus())) {
            return Map.of("success", false, "message", "Template is not on shelf");
        }

        TemplateOrderEntity order = findSuccessfulOrder(userId, templateId);
        if (!isFree(template) && order == null) {
            return Map.of("success", false, "message", "Please purchase this template before use");
        }

        PromptTemplateVersionEntity currentVersion = resolveCurrentVersion(template);
        TemplateUseLogEntity log = new TemplateUseLogEntity();
        log.setUserId(userId);
        log.setTemplateId(templateId);
        log.setVersionId(currentVersion.getVersionId());
        log.setOrderId(order == null ? null : order.getOrderId());
        log.setInputSummary(inputSummary);
        log.setUseSource("WEB");
        log.setUsedAt(LocalDateTime.now());
        templateUseLogMapper.insert(log);

        incrementTemplateCounter(templateId, "use_count", 1);
        PromptTemplateEntity updatedTemplate = promptTemplateMapper.selectById(templateId);
        String usageResult = "【输入摘要】\n" + inputSummary + "\n\n【套用模板】\n" + currentVersion.getPromptContent();
        return Map.of(
                "success", true,
                "message", "模板使用成功，已写入使用日志",
                "promptContent", currentVersion.getPromptContent(),
                "usageResult", usageResult,
                "useCount", updatedTemplate.getUseCount());
    }

    @Override
    @Transactional
    public Map<String, Object> submitReview(Long userId, ReviewRequest request) {
        PromptTemplateEntity template = promptTemplateMapper.selectById(request.getTemplateId());
        if (template == null) {
            return Map.of("success", false, "message", "Template not found");
        }

        TemplateUseLogEntity useLog = templateUseLogMapper.selectList(new LambdaQueryWrapper<TemplateUseLogEntity>()
                        .eq(TemplateUseLogEntity::getUserId, userId)
                        .eq(TemplateUseLogEntity::getTemplateId, request.getTemplateId())
                        .orderByDesc(TemplateUseLogEntity::getUsedAt))
                .stream()
                .findFirst()
                .orElse(null);
        if (useLog == null) {
            return Map.of("success", false, "message", "Use the template before reviewing");
        }

        Long existingReviewCount = templateReviewMapper.selectCount(new LambdaQueryWrapper<TemplateReviewEntity>()
                .eq(TemplateReviewEntity::getUseLogId, useLog.getUseLogId()));
        if (existingReviewCount > 0) {
            return Map.of("success", false, "message", "This usage has already been reviewed");
        }

        TemplateReviewEntity review = new TemplateReviewEntity();
        review.setUserId(userId);
        review.setTemplateId(request.getTemplateId());
        review.setUseLogId(useLog.getUseLogId());
        review.setScore(request.getScore());
        review.setReviewContent(request.getContent());
        review.setReviewStatus("VISIBLE");
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        templateReviewMapper.insert(review);

        List<TemplateReviewEntity> reviews = templateReviewMapper.selectList(new LambdaQueryWrapper<TemplateReviewEntity>()
                .eq(TemplateReviewEntity::getTemplateId, request.getTemplateId()));
        double average = reviews.stream().mapToInt(TemplateReviewEntity::getScore).average().orElse(0D);
        template.setReviewCount(reviews.size());
        template.setAvgScore(BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP).doubleValue());
        promptTemplateMapper.updateById(template);
        return Map.of("success", true, "message", "Review submitted", "avgScore", template.getAvgScore(), "reviewCount", template.getReviewCount());
    }

    @Override
    public List<OrderView> listOrders(Long userId) {
        Map<Long, String> templateMap = promptTemplateMapper.selectList(new LambdaQueryWrapper<PromptTemplateEntity>())
                .stream()
                .collect(Collectors.toMap(PromptTemplateEntity::getTemplateId, PromptTemplateEntity::getTitle));
        return templateOrderMapper.selectList(new LambdaQueryWrapper<TemplateOrderEntity>()
                        .eq(TemplateOrderEntity::getUserId, userId)
                        .orderByDesc(TemplateOrderEntity::getCreatedAt))
                .stream()
                .map(order -> OrderView.builder()
                        .templateId(order.getTemplateId())
                        .orderNo(order.getOrderNo())
                        .templateTitle(templateMap.getOrDefault(order.getTemplateId(), "Unknown template"))
                        .payAmount(order.getPayAmount())
                        .payStatus(order.getPayStatus())
                        .createdAt(order.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    public List<UsageLogView> listUsageLogs(Long userId) {
        Map<Long, String> templateMap = promptTemplateMapper.selectList(new LambdaQueryWrapper<PromptTemplateEntity>())
                .stream()
                .collect(Collectors.toMap(PromptTemplateEntity::getTemplateId, PromptTemplateEntity::getTitle));
        return templateUseLogMapper.selectList(new LambdaQueryWrapper<TemplateUseLogEntity>()
                        .eq(TemplateUseLogEntity::getUserId, userId)
                        .orderByDesc(TemplateUseLogEntity::getUsedAt))
                .stream()
                .map(log -> UsageLogView.builder()
                        .templateTitle(templateMap.getOrDefault(log.getTemplateId(), "Unknown template"))
                        .inputSummary(log.getInputSummary())
                        .usedAt(log.getUsedAt())
                        .build())
                .toList();
    }

    @Override
    public List<BalanceLogView> listBalanceLogs(Long userId) {
        return userBalanceLogMapper.selectList(new LambdaQueryWrapper<UserBalanceLogEntity>()
                        .eq(UserBalanceLogEntity::getUserId, userId)
                        .orderByDesc(UserBalanceLogEntity::getCreatedAt))
                .stream()
                .map(log -> BalanceLogView.builder()
                        .changeType(log.getChangeType())
                        .changeAmount(log.getChangeAmount())
                        .balanceAfter(log.getBalanceAfter())
                        .bizType(log.getBizType())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }

    private TemplateFavoriteEntity findFavorite(Long userId, Long templateId) {
        return templateFavoriteMapper.selectOne(new LambdaQueryWrapper<TemplateFavoriteEntity>()
                .eq(TemplateFavoriteEntity::getUserId, userId)
                .eq(TemplateFavoriteEntity::getTemplateId, templateId));
    }

    private TemplateOrderEntity findSuccessfulOrder(Long userId, Long templateId) {
        return templateOrderMapper.selectOne(new LambdaQueryWrapper<TemplateOrderEntity>()
                .eq(TemplateOrderEntity::getUserId, userId)
                .eq(TemplateOrderEntity::getTemplateId, templateId)
                .eq(TemplateOrderEntity::getOrderStatus, "SUCCESS")
                .in(TemplateOrderEntity::getPayStatus, List.of("PAID", "FREE_CLAIMED", "FREE_CAMPAIGN"))
                .orderByDesc(TemplateOrderEntity::getCreatedAt)
                .last("LIMIT 1"));
    }

    private CampaignTemplateEntity findActiveCampaignTemplate(Long templateId) {
        LocalDateTime now = LocalDateTime.now();
        List<FreeCampaignEntity> activeCampaigns = freeCampaignMapper.selectList(new LambdaQueryWrapper<FreeCampaignEntity>()
                .eq(FreeCampaignEntity::getCampaignStatus, "ACTIVE")
                .le(FreeCampaignEntity::getStartTime, now)
                .ge(FreeCampaignEntity::getEndTime, now));
        if (activeCampaigns.isEmpty()) {
            return null;
        }
        List<Long> campaignIds = activeCampaigns.stream().map(FreeCampaignEntity::getCampaignId).toList();
        return campaignTemplateMapper.selectList(new LambdaQueryWrapper<CampaignTemplateEntity>()
                        .in(CampaignTemplateEntity::getCampaignId, campaignIds)
                        .eq(CampaignTemplateEntity::getTemplateId, templateId)
                        .gt(CampaignTemplateEntity::getRemainingQuota, 0)
                        .orderByDesc(CampaignTemplateEntity::getCreatedAt))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> claimCampaignTemplate(SysUserEntity user, PromptTemplateEntity template, CampaignTemplateEntity campaignTemplate) {
        CampaignClaimEntity existingClaim = campaignClaimMapper.selectOne(new LambdaQueryWrapper<CampaignClaimEntity>()
                .eq(CampaignClaimEntity::getCampaignTemplateId, campaignTemplate.getCampaignTemplateId())
                .eq(CampaignClaimEntity::getUserId, user.getUserId())
                .last("LIMIT 1"));
        if (existingClaim != null && existingClaim.getOrderId() != null) {
            TemplateOrderEntity order = templateOrderMapper.selectById(existingClaim.getOrderId());
            return Map.of("success", true,
                    "message", "已领取过该免费活动模板",
                    "order", toOrderView(order, template),
                    "balance", user.getBalance());
        }

        TemplateOrderEntity order = new TemplateOrderEntity();
        order.setOrderNo("ORD" + System.currentTimeMillis());
        order.setUserId(user.getUserId());
        order.setTemplateId(template.getTemplateId());
        order.setOriginAmount(template.getPrice());
        order.setPayAmount(BigDecimal.ZERO);
        order.setPayStatus("FREE_CAMPAIGN");
        order.setOrderStatus("SUCCESS");
        order.setPayTime(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        templateOrderMapper.insert(order);

        CampaignClaimEntity claim = new CampaignClaimEntity();
        claim.setCampaignTemplateId(campaignTemplate.getCampaignTemplateId());
        claim.setUserId(user.getUserId());
        claim.setOrderId(order.getOrderId());
        claim.setClaimStatus("SUCCESS");
        claim.setClaimTime(LocalDateTime.now());
        campaignClaimMapper.insert(claim);

        campaignTemplateMapper.update(null, new LambdaUpdateWrapper<CampaignTemplateEntity>()
                .eq(CampaignTemplateEntity::getCampaignTemplateId, campaignTemplate.getCampaignTemplateId())
                .gt(CampaignTemplateEntity::getRemainingQuota, 0)
                .setSql("remaining_quota = remaining_quota - 1"));

        return Map.of("success", true,
                "message", "限时免费活动领取成功",
                "order", toOrderView(order, template),
                "balance", user.getBalance());
    }

    private PromptTemplateVersionEntity resolveCurrentVersion(PromptTemplateEntity template) {
        if (template.getCurrentVersionId() != null) {
            PromptTemplateVersionEntity currentVersion = versionMapper.selectById(template.getCurrentVersionId());
            if (currentVersion != null) {
                return currentVersion;
            }
        }
        return versionMapper.selectList(new LambdaQueryWrapper<PromptTemplateVersionEntity>()
                        .eq(PromptTemplateVersionEntity::getTemplateId, template.getTemplateId())
                        .orderByDesc(PromptTemplateVersionEntity::getCreatedAt))
                .stream()
                .findFirst()
                .orElseThrow();
    }

    private void incrementTemplateCounter(Long templateId, String columnName, int delta) {
        String operator = delta >= 0 ? "+" : "-";
        promptTemplateMapper.update(null, new LambdaUpdateWrapper<PromptTemplateEntity>()
                .eq(PromptTemplateEntity::getTemplateId, templateId)
                .setSql(columnName + " = " + columnName + " " + operator + " " + Math.abs(delta)));
    }

    private boolean isFree(PromptTemplateEntity template) {
        return "FREE".equalsIgnoreCase(template.getPriceType()) || template.getPrice().compareTo(BigDecimal.ZERO) == 0;
    }

    private OrderView toOrderView(TemplateOrderEntity order, PromptTemplateEntity template) {
        return OrderView.builder()
                .templateId(template.getTemplateId())
                .orderNo(order.getOrderNo())
                .templateTitle(template.getTitle())
                .payAmount(order.getPayAmount())
                .payStatus(order.getPayStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public Map<String, Object> setTemplateFree(Long templateId) {
        PromptTemplateEntity template = promptTemplateMapper.selectById(templateId);
        if (template == null) return Map.of("success", false, "message", "模板不存在");
        LocalDateTime now = LocalDateTime.now();
        FreeCampaignEntity campaign = freeCampaignMapper.selectList(new LambdaQueryWrapper<FreeCampaignEntity>()
                        .eq(FreeCampaignEntity::getCampaignName, "平台限时免费活动")
                        .eq(FreeCampaignEntity::getCampaignStatus, "ACTIVE")
                        .le(FreeCampaignEntity::getStartTime, now)
                        .ge(FreeCampaignEntity::getEndTime, now)
                        .orderByDesc(FreeCampaignEntity::getCreatedAt))
                .stream()
                .findFirst()
                .orElse(null);
        if (campaign == null) {
            campaign = new FreeCampaignEntity();
            campaign.setCampaignName("平台限时免费活动");
            campaign.setStartTime(now);
            campaign.setEndTime(now.plusDays(7));
            campaign.setCampaignStatus("ACTIVE");
            campaign.setCreatedAt(now);
            freeCampaignMapper.insert(campaign);
        }

        CampaignTemplateEntity relation = campaignTemplateMapper.selectOne(new LambdaQueryWrapper<CampaignTemplateEntity>()
                .eq(CampaignTemplateEntity::getCampaignId, campaign.getCampaignId())
                .eq(CampaignTemplateEntity::getTemplateId, templateId)
                .last("LIMIT 1"));
        if (relation == null) {
            relation = new CampaignTemplateEntity();
            relation.setCampaignId(campaign.getCampaignId());
            relation.setTemplateId(templateId);
            relation.setTotalQuota(100);
            relation.setRemainingQuota(100);
            relation.setPerUserLimit(1);
            relation.setCreatedAt(now);
            campaignTemplateMapper.insert(relation);
            return Map.of("success", true, "message", "已加入限时免费活动，库存 100 份");
        }
        relation.setTotalQuota(relation.getTotalQuota() + 100);
        relation.setRemainingQuota(relation.getRemainingQuota() + 100);
        campaignTemplateMapper.updateById(relation);
        return Map.of("success", true, "message", "已补充限时免费活动库存 100 份");
    }
}
