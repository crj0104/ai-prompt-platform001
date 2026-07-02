package com.course.promptplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.course.promptplatform.entity.PromptTemplateEntity;
import com.course.promptplatform.entity.PromptTemplateVersionEntity;
import com.course.promptplatform.entity.SysUserEntity;
import com.course.promptplatform.entity.TemplateFavoriteEntity;
import com.course.promptplatform.entity.TemplateOrderEntity;
import com.course.promptplatform.entity.TemplateReviewEntity;
import com.course.promptplatform.entity.TemplateUseLogEntity;
import com.course.promptplatform.entity.UserBalanceLogEntity;
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
        return Map.of(
                "success", true,
                "message", "Usage saved",
                "promptContent", currentVersion.getPromptContent(),
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
                .in(TemplateOrderEntity::getPayStatus, List.of("PAID", "FREE_CLAIMED"))
                .orderByDesc(TemplateOrderEntity::getCreatedAt)
                .last("LIMIT 1"));
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
                .orderNo(order.getOrderNo())
                .templateTitle(template.getTitle())
                .payAmount(order.getPayAmount())
                .payStatus(order.getPayStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
