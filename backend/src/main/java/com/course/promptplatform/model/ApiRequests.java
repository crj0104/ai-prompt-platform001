package com.course.promptplatform.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 统一存放演示项目的接口入参对象，减少样板文件数量。
 */
public final class ApiRequests {

    private ApiRequests() {
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        private String phone;

        @Email(message = "邮箱格式不正确")
        private String email;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "账号不能为空")
        private String account;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class SearchRequest {
        private String keyword;
        private String tag;
        private Double minScore;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private String sort;
        private Long current;
        private Long size;
    }

    @Data
    public static class ReviewRequest {
        @NotNull(message = "模板 ID 不能为空")
        private Long templateId;

        @NotNull(message = "评分不能为空")
        @Min(value = 1, message = "评分最低为 1")
        @Max(value = 5, message = "评分最高为 5")
        private Integer score;

        @NotBlank(message = "评价内容不能为空")
        private String content;
    }

    @Data
    public static class UseTemplateRequest {
        @NotBlank(message = "输入摘要不能为空")
        private String inputSummary;
    }

    @Data
    public static class PurchaseRequest {
        @NotNull(message = "模板 ID 不能为空")
        private Long templateId;
    }

    @Data
    public static class PublishTemplateRequest {
        @NotBlank(message = "标题不能为空")
        private String title;

        private String sceneDesc;

        @NotBlank(message = "提示词内容不能为空")
        private String promptContent;

        private java.util.List<String> tags;

        private BigDecimal price;

        private String priceType;
    }

    @Data
    public static class UpdateTemplateRequest {
        @NotBlank(message = "标题不能为空")
        private String title;

        private String sceneDesc;

        @NotBlank(message = "提示词内容不能为空")
        private String promptContent;

        private java.util.List<String> tags;

        private BigDecimal price;

        private String priceType;
    }
}
