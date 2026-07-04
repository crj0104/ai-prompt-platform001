package com.course.promptplatform.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 页面渲染与接口输出共用的轻量数据模型。
 */
public final class PortalViewModels {

    private PortalViewModels() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagView {
        private Long id;
        private String name;
        private String parentName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateCardView {
        private Long id;
        private String title;
        private String scene;
        private String summary;
        private BigDecimal price;
        private boolean free;
        private String status;
        private Integer useCount;
        private Integer favoriteCount;
        private Double avgScore;
        private List<String> tags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionView {
        private Long id;
        private String versionNo;
        private String changeNote;
        private String promptContent;
        private String editorName;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewView {
        private Long id;
        private String username;
        private Integer score;
        private String content;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateDetailView {
        private Long id;
        private String title;
        private String scene;
        private String promptContent;
        private String creatorName;
        private BigDecimal price;
        private boolean free;
        private String status;
        private Integer useCount;
        private Integer favoriteCount;
        private Double avgScore;
        private Integer reviewCount;
        private List<String> tags;
        private List<VersionView> versions;
        private List<ReviewView> reviews;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatCardView {
        private String label;
        private String value;
        private String trend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderView {
        private Long templateId;
        private String orderNo;
        private String templateTitle;
        private BigDecimal payAmount;
        private String payStatus;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageLogView {
        private String templateTitle;
        private String inputSummary;
        private LocalDateTime usedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceLogView {
        private String changeType;
        private BigDecimal changeAmount;
        private BigDecimal balanceAfter;
        private String bizType;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignView {
        private Long id;
        private String name;
        private String templateTitle;
        private Integer totalQuota;
        private Integer remainingQuota;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyIncomeView {
        private String month;
        private BigDecimal income;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateIncomeView {
        private Long templateId;
        private String title;
        private BigDecimal income;
        private Integer orderCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrendView {
        private LocalDate date;
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateRankView {
        private Integer rankNo;
        private Long templateId;
        private String title;
        private String scene;
        private BigDecimal price;
        private boolean free;
        private Integer useCount;
        private Integer favoriteCount;
        private Double avgScore;
        private Integer recentUseCount;
        private BigDecimal hotScore;
        private List<String> tags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfileView {
        private Long userId;
        private String username;
        private String phone;
        private String email;
        private String creatorLevel;
        private BigDecimal balance;
        private List<TemplateCardView> publishedTemplates;
        private List<TemplateCardView> favorites;
        private List<OrderView> orders;
        private List<UsageLogView> usageLogs;
        private List<BalanceLogView> balanceLogs;
    }
}
