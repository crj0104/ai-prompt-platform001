package com.course.promptplatform.controller;

import com.course.promptplatform.common.CurrentUser;
import com.course.promptplatform.model.ApiRequests.LoginRequest;
import com.course.promptplatform.model.ApiRequests.PublishTemplateRequest;
import com.course.promptplatform.model.ApiRequests.PurchaseRequest;
import com.course.promptplatform.model.ApiRequests.RegisterRequest;
import com.course.promptplatform.model.ApiRequests.ReviewRequest;
import com.course.promptplatform.model.ApiRequests.SearchRequest;
import com.course.promptplatform.model.ApiRequests.UpdateTemplateRequest;
import com.course.promptplatform.model.ApiRequests.UseTemplateRequest;
import com.course.promptplatform.service.PortalService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供JSON 接口。
 */
@Validated
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ApiController {

    private final PortalService portalService;

    @PostMapping("/auth/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        return portalService.register(request);
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        return portalService.login(request);
    }

    @GetMapping("/templates/search")
    public Object search(SearchRequest request) {
        long current = request.getCurrent() == null ? 1L : request.getCurrent();
        long size = request.getSize() == null ? 9L : request.getSize();
        return portalService.searchTemplates(request, current, size);
    }

    @GetMapping("/templates")
    public Object templates() {
        return portalService.listTemplates();
    }

    @GetMapping("/templates/{id}")
    public Object templateDetail(@PathVariable Long id) {
        return portalService.getTemplateDetail(id);
    }

    @PostMapping("/templates")
    public Map<String, Object> publishTemplate(@Valid @RequestBody PublishTemplateRequest request) {
        return portalService.publishTemplate(CurrentUser.userId(), request);
    }

    @PutMapping("/templates/{id}")
    public Map<String, Object> updateTemplate(@PathVariable Long id,
                                              @Valid @RequestBody UpdateTemplateRequest request) {
        return portalService.updateTemplate(CurrentUser.userId(), id, request);
    }

    @DeleteMapping("/templates/{id}")
    public Map<String, Object> deleteTemplate(@PathVariable Long id) {
        return portalService.deleteTemplate(CurrentUser.userId(), id);
    }

    @GetMapping("/profile")
    public Object profile() {
        return portalService.getProfile(CurrentUser.userId());
    }

    @PostMapping("/users/creator-apply")
    public Map<String, Object> upgrade() {
        return portalService.upgradeToCreator(CurrentUser.userId());
    }

    @GetMapping("/tags")
    public Object tags() {
        return portalService.allTags();
    }

    @PostMapping("/templates/{id}/favorite")
    public Map<String, Object> favorite(@PathVariable Long id) {
        return portalService.toggleFavorite(CurrentUser.userId(), id);
    }

    @DeleteMapping("/templates/{id}/favorite")
    public Map<String, Object> unfavorite(@PathVariable Long id) {
        return portalService.removeFavorite(CurrentUser.userId(), id);
    }

    @PostMapping("/orders")
    public Map<String, Object> purchase(@Valid @RequestBody PurchaseRequest request) {
        return portalService.purchaseTemplate(CurrentUser.userId(), request);
    }

    @PostMapping("/templates/{id}/use")
    public Map<String, Object> useTemplate(@PathVariable Long id,
                                           @Valid @RequestBody UseTemplateRequest request) {
        return portalService.useTemplate(CurrentUser.userId(), id, request.getInputSummary());
    }

    @PostMapping("/reviews")
    public Map<String, Object> review(@Valid @RequestBody ReviewRequest request) {
        return portalService.submitReview(CurrentUser.userId(), request);
    }

    @GetMapping("/stats/creator")
    public Map<String, Object> creatorStats() {
        return portalService.creatorDashboard(CurrentUser.userId());
    }

    @GetMapping("/stats/platform")
    public Map<String, Object> platformStats() {
        return portalService.adminDashboard();
    }

    @PostMapping("/admin/templates/{id}/free")
    public Map<String, Object> setFree(@PathVariable Long id) {
        return portalService.setTemplateFree(id);
    }

    @GetMapping("/recommend/templates")
    public Object recommend() {
        return portalService.recommendTemplates();
    }

    @GetMapping("/templates/{id}/usage-trend")
    public Object usageTrend(@PathVariable Long id, Long days) {
        int safeDays = days == null ? 7 : Math.max(1, days.intValue());
        return portalService.queryUsageTrend(id, safeDays);
    }

    @GetMapping("/templates/hot-ranking")
    public Object hotRanking(Integer days, Integer limit) {
        int safeDays = days == null ? 7 : Math.max(1, days);
        int safeLimit = limit == null ? 10 : Math.max(1, limit);
        return portalService.queryHotRanking(safeDays, safeLimit);
    }

}
