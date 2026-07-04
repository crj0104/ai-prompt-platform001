package com.course.promptplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.course.promptplatform.entity.PromptTemplateEntity;
import com.course.promptplatform.entity.PromptTemplateVersionEntity;
import com.course.promptplatform.entity.SysUserEntity;
import com.course.promptplatform.entity.TagEntity;
import com.course.promptplatform.entity.TemplateFavoriteEntity;
import com.course.promptplatform.entity.TemplateReviewEntity;
import com.course.promptplatform.entity.TemplateTagRelEntity;
import com.course.promptplatform.mapper.PromptTemplateMapper;
import com.course.promptplatform.mapper.PromptTemplateVersionMapper;
import com.course.promptplatform.mapper.SysUserMapper;
import com.course.promptplatform.mapper.TagMapper;
import com.course.promptplatform.mapper.TemplateFavoriteMapper;
import com.course.promptplatform.mapper.TemplateReviewMapper;
import com.course.promptplatform.mapper.TemplateTagRelMapper;
import com.course.promptplatform.model.ApiRequests.PublishTemplateRequest;
import com.course.promptplatform.model.ApiRequests.SearchRequest;
import com.course.promptplatform.model.ApiRequests.UpdateTemplateRequest;
import com.course.promptplatform.model.PortalViewModels.ReviewView;
import com.course.promptplatform.model.PortalViewModels.TemplateCardView;
import com.course.promptplatform.model.PortalViewModels.TemplateDetailView;
import com.course.promptplatform.model.PortalViewModels.VersionView;
import com.course.promptplatform.model.query.TemplateQueryRow;
import com.course.promptplatform.service.TemplateDomainService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class TemplateDomainServiceImpl implements TemplateDomainService {

    private final PromptTemplateMapper promptTemplateMapper;
    private final PromptTemplateVersionMapper versionMapper;
    private final TemplateTagRelMapper templateTagRelMapper;
    private final TagMapper tagMapper;
    private final TemplateReviewMapper reviewMapper;
    private final SysUserMapper sysUserMapper;
    private final TemplateFavoriteMapper templateFavoriteMapper;

    @Override
    public List<TemplateCardView> listTemplates() {
        SearchRequest request = new SearchRequest();
        request.setSort("hot");
        return buildTemplateCardsFromRows(promptTemplateMapper.searchTemplatePage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 9), request).getRecords());
    }

    @Override
    public Map<String, Object> searchTemplates(SearchRequest request, long current, long size) {
        long pageCurrent = request.getCurrent() == null ? current : request.getCurrent();
        long pageSize = request.getSize() == null ? size : request.getSize();
        com.baomidou.mybatisplus.core.metadata.IPage<TemplateQueryRow> page =
                promptTemplateMapper.searchTemplatePage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageCurrent, pageSize), request);
        List<TemplateCardView> records = buildTemplateCardsFromRows(page.getRecords());
        return Map.of(
                "records", records,
                "current", page.getCurrent(),
                "size", page.getSize(),
                "total", page.getTotal(),
                "pages", page.getPages());
    }

    @Override
    public TemplateDetailView getTemplateDetail(Long templateId) {
        PromptTemplateEntity entity = promptTemplateMapper.selectById(templateId);
        if (entity == null) {
            entity = promptTemplateMapper.selectList(new LambdaQueryWrapper<PromptTemplateEntity>()
                            .orderByDesc(PromptTemplateEntity::getUseCount))
                    .stream().findFirst().orElseThrow();
        }
        List<VersionView> versions = buildVersions(List.of(entity.getTemplateId()));
        List<ReviewView> reviews = buildReviews(List.of(entity.getTemplateId()));
        String creatorName = resolveUserMap(Set.of(entity.getCreatorUserId())).getOrDefault(entity.getCreatorUserId(), "未知创作者");
        String promptContent = resolveCurrentPrompt(entity, versions);
        return TemplateDetailView.builder()
                .id(entity.getTemplateId())
                .title(entity.getTitle())
                .scene(entity.getSceneDesc())
                .promptContent(promptContent)
                .creatorName(creatorName)
                .price(entity.getPrice())
                .free(isFree(entity))
                .status(resolveStatus(entity.getShelfStatus()))
                .useCount(entity.getUseCount())
                .favoriteCount(entity.getFavoriteCount())
                .avgScore(entity.getAvgScore())
                .reviewCount(entity.getReviewCount())
                .tags(resolveTagsByTemplateIds(List.of(entity.getTemplateId())).getOrDefault(entity.getTemplateId(), List.of()))
                .versions(versions)
                .reviews(reviews)
                .build();
    }

    @Override
    public List<TemplateCardView> recommendTemplates() {
        SearchRequest request = new SearchRequest();
        request.setSort("score");
        return buildTemplateCardsFromRows(promptTemplateMapper.searchTemplatePage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 3), request).getRecords());
    }

    @Override
    public List<String> allTags() {
        return tagMapper.selectList(new LambdaQueryWrapper<TagEntity>().orderByAsc(TagEntity::getSortNo, TagEntity::getTagName))
                .stream().map(TagEntity::getTagName).distinct().toList();
    }

    @Override
    public List<TemplateCardView> publishedTemplates(Long userId) {
        List<PromptTemplateEntity> entities = promptTemplateMapper.selectList(
                new LambdaQueryWrapper<PromptTemplateEntity>()
                        .eq(PromptTemplateEntity::getCreatorUserId, userId)
                        .ne(PromptTemplateEntity::getShelfStatus, "DELETED")
                        .orderByDesc(PromptTemplateEntity::getUpdatedAt));
        return buildTemplateCardsFromEntities(entities);
    }

    @Override
    public List<TemplateCardView> favoriteTemplates(Long userId) {
        List<TemplateFavoriteEntity> favorites = templateFavoriteMapper.selectList(
                new LambdaQueryWrapper<TemplateFavoriteEntity>().eq(TemplateFavoriteEntity::getUserId, userId));
        if (favorites.isEmpty()) {
            return List.of();
        }
        List<Long> templateIds = favorites.stream().map(TemplateFavoriteEntity::getTemplateId).toList();
        List<PromptTemplateEntity> entities = promptTemplateMapper.selectBatchIds(templateIds);
        return buildTemplateCardsFromEntities(entities).stream()
                .sorted(Comparator.comparing(TemplateCardView::getUseCount).reversed())
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> publishTemplate(Long userId, PublishTemplateRequest request) {
        SysUserEntity user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Map.of("success", false, "message", "用户不存在");
        }
        if (user.getCreatorLevel() == null || user.getCreatorLevel().isBlank()) {
            return Map.of("success", false, "message", "请先开通创作者身份");
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal price = request.getPrice() == null ? BigDecimal.ZERO : request.getPrice();
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            return Map.of("success", false, "message", "价格不能小于 0");
        }
        String priceType = price.compareTo(BigDecimal.ZERO) <= 0 ? "FREE" : "PAID";
        if ("FREE".equalsIgnoreCase(request.getPriceType())) {
            priceType = "FREE";
            price = BigDecimal.ZERO;
        }
        String sceneDesc = request.getSceneDesc() == null || request.getSceneDesc().isBlank()
                ? request.getTitle()
                : request.getSceneDesc();

        PromptTemplateEntity template = new PromptTemplateEntity();
        template.setCreatorUserId(userId);
        template.setTitle(request.getTitle());
        template.setSceneDesc(sceneDesc);
        template.setPriceType(priceType);
        template.setPrice(price);
        template.setShelfStatus("ON_SHELF");
        template.setUseCount(0);
        template.setFavoriteCount(0);
        template.setAvgScore(0.0);
        template.setReviewCount(0);
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        promptTemplateMapper.insert(template);

        PromptTemplateVersionEntity version = new PromptTemplateVersionEntity();
        version.setTemplateId(template.getTemplateId());
        version.setVersionNo("v1.0");
        version.setPromptContent(request.getPromptContent());
        version.setChangeNote("首次发布");
        version.setEditorUserId(userId);
        version.setCreatedAt(now);
        versionMapper.insert(version);

        template.setCurrentVersionId(version.getVersionId());
        promptTemplateMapper.updateById(template);

        bindTags(template.getTemplateId(), request.getTags(), now);
        return Map.of("success", true,
                "message", "模板发布成功",
                "templateId", template.getTemplateId(),
                "versionId", version.getVersionId());
    }

    @Override
    @Transactional
    public Map<String, Object> updateTemplate(Long userId, Long templateId, UpdateTemplateRequest request) {
        PromptTemplateEntity template = promptTemplateMapper.selectById(templateId);
        if (template == null || "DELETED".equalsIgnoreCase(template.getShelfStatus())) {
            return Map.of("success", false, "message", "模板不存在");
        }
        if (!userId.equals(template.getCreatorUserId())) {
            return Map.of("success", false, "message", "只能修改自己发布的模板");
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal price = request.getPrice() == null ? BigDecimal.ZERO : request.getPrice();
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            return Map.of("success", false, "message", "价格不能小于 0");
        }
        String priceType = price.compareTo(BigDecimal.ZERO) <= 0 ? "FREE" : "PAID";
        if ("FREE".equalsIgnoreCase(request.getPriceType())) {
            priceType = "FREE";
            price = BigDecimal.ZERO;
        }

        template.setTitle(request.getTitle());
        template.setSceneDesc(request.getSceneDesc() == null || request.getSceneDesc().isBlank()
                ? request.getTitle()
                : request.getSceneDesc());
        template.setPriceType(priceType);
        template.setPrice(price);
        template.setUpdatedAt(now);

        PromptTemplateVersionEntity version = new PromptTemplateVersionEntity();
        version.setTemplateId(templateId);
        version.setVersionNo(nextVersionNo(templateId));
        version.setPromptContent(request.getPromptContent());
        version.setChangeNote("创作者修改模板");
        version.setEditorUserId(userId);
        version.setSourceVersionId(template.getCurrentVersionId());
        version.setCreatedAt(now);
        versionMapper.insert(version);

        template.setCurrentVersionId(version.getVersionId());
        promptTemplateMapper.updateById(template);
        replaceTags(templateId, request.getTags(), now);

        return Map.of("success", true,
                "message", "模板修改成功",
                "templateId", templateId,
                "versionId", version.getVersionId());
    }

    @Override
    @Transactional
    public Map<String, Object> deleteTemplate(Long userId, Long templateId) {
        PromptTemplateEntity template = promptTemplateMapper.selectById(templateId);
        if (template == null || "DELETED".equalsIgnoreCase(template.getShelfStatus())) {
            return Map.of("success", false, "message", "模板不存在");
        }
        if (!userId.equals(template.getCreatorUserId())) {
            return Map.of("success", false, "message", "只能删除自己发布的模板");
        }
        template.setShelfStatus("DELETED");
        template.setUpdatedAt(LocalDateTime.now());
        promptTemplateMapper.updateById(template);
        return Map.of("success", true, "message", "模板已删除");
    }

    private List<TemplateCardView> buildTemplateCardsFromEntities(List<PromptTemplateEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        List<Long> templateIds = entities.stream().map(PromptTemplateEntity::getTemplateId).toList();
        Map<Long, List<String>> tagMap = resolveTagsByTemplateIds(templateIds);
        Map<Long, String> promptSummaryMap = resolveCurrentPromptMap(entities);
        return entities.stream().map(entity -> TemplateCardView.builder()
                        .id(entity.getTemplateId())
                        .title(entity.getTitle())
                        .scene(entity.getSceneDesc())
                        .summary(promptSummaryMap.getOrDefault(entity.getTemplateId(), entity.getSceneDesc()))
                        .price(entity.getPrice())
                        .free(isFree(entity))
                        .status(resolveStatus(entity.getShelfStatus()))
                        .useCount(entity.getUseCount())
                        .favoriteCount(entity.getFavoriteCount())
                        .avgScore(entity.getAvgScore())
                        .tags(tagMap.getOrDefault(entity.getTemplateId(), List.of()))
                        .build())
                .toList();
    }

    private List<TemplateCardView> buildTemplateCardsFromRows(List<TemplateQueryRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> templateIds = rows.stream().map(TemplateQueryRow::getTemplateId).toList();
        Map<Long, List<String>> tagMap = resolveTagsByTemplateIds(templateIds);
        return rows.stream().map(row -> TemplateCardView.builder()
                        .id(row.getTemplateId())
                        .title(row.getTitle())
                        .scene(row.getSceneDesc())
                        .summary(row.getSummary() == null || row.getSummary().isBlank() ? row.getSceneDesc() : row.getSummary())
                        .price(row.getPrice())
                        .free(isFree(row.getPriceType(), row.getPrice()))
                        .status(resolveStatus(row.getShelfStatus()))
                        .useCount(row.getUseCount())
                        .favoriteCount(row.getFavoriteCount())
                        .avgScore(row.getAvgScore())
                        .tags(tagMap.getOrDefault(row.getTemplateId(), List.of()))
                        .build())
                .toList();
    }

    private Map<Long, List<String>> resolveTagsByTemplateIds(List<Long> templateIds) {
        List<TemplateTagRelEntity> relations = templateTagRelMapper.selectList(
                new LambdaQueryWrapper<TemplateTagRelEntity>().in(TemplateTagRelEntity::getTemplateId, templateIds));
        if (relations.isEmpty()) {
            return Map.of();
        }
        List<Long> tagIds = relations.stream().map(TemplateTagRelEntity::getTagId).distinct().toList();
        Map<Long, String> tagMap = tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(TagEntity::getTagId, TagEntity::getTagName));
        return relations.stream().collect(Collectors.groupingBy(
                TemplateTagRelEntity::getTemplateId,
                Collectors.mapping(rel -> tagMap.get(rel.getTagId()), Collectors.toList())));
    }

    private void bindTags(Long templateId, List<String> rawTags, LocalDateTime now) {
        if (rawTags == null || rawTags.isEmpty()) {
            return;
        }
        List<String> tagNames = rawTags.stream()
                .map(tag -> tag == null ? "" : tag.trim())
                .filter(tag -> !tag.isBlank())
                .distinct()
                .limit(5)
                .toList();
        for (String tagName : tagNames) {
            TagEntity tag = tagMapper.selectOne(new LambdaQueryWrapper<TagEntity>()
                    .eq(TagEntity::getTagName, tagName)
                    .last("LIMIT 1"));
            if (tag == null) {
                tag = new TagEntity();
                tag.setTagName(tagName);
                tag.setTagLevel(1);
                tag.setTagPath(tagName);
                tag.setSortNo(99);
                tag.setCreatedAt(now);
                tagMapper.insert(tag);
            }
            TemplateTagRelEntity rel = new TemplateTagRelEntity();
            rel.setTemplateId(templateId);
            rel.setTagId(tag.getTagId());
            rel.setCreatedAt(now);
            templateTagRelMapper.insert(rel);
        }
    }

    private void replaceTags(Long templateId, List<String> rawTags, LocalDateTime now) {
        templateTagRelMapper.delete(new LambdaQueryWrapper<TemplateTagRelEntity>()
                .eq(TemplateTagRelEntity::getTemplateId, templateId));
        bindTags(templateId, rawTags, now);
    }

    private String nextVersionNo(Long templateId) {
        Long count = versionMapper.selectCount(new LambdaQueryWrapper<PromptTemplateVersionEntity>()
                .eq(PromptTemplateVersionEntity::getTemplateId, templateId));
        return "v" + (count + 1) + ".0";
    }

    private Map<Long, String> resolveCurrentPromptMap(List<PromptTemplateEntity> templates) {
        List<Long> currentVersionIds = templates.stream()
                .map(PromptTemplateEntity::getCurrentVersionId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (currentVersionIds.isEmpty()) {
            return Map.of();
        }
        return versionMapper.selectBatchIds(currentVersionIds).stream()
                .collect(Collectors.toMap(
                        PromptTemplateVersionEntity::getTemplateId,
                        PromptTemplateVersionEntity::getPromptContent,
                        (left, right) -> left));
    }

    private String resolveCurrentPrompt(PromptTemplateEntity template, List<VersionView> versions) {
        if (template.getCurrentVersionId() != null) {
            return versions.stream()
                    .filter(version -> template.getCurrentVersionId().equals(version.getId()))
                    .map(VersionView::getPromptContent)
                    .findFirst()
                    .orElseGet(() -> fallbackPrompt(versions));
        }
        return fallbackPrompt(versions);
    }

    private String fallbackPrompt(List<VersionView> versions) {
        return versions.isEmpty() ? "No version content" : versions.get(versions.size() - 1).getPromptContent();
    }

    private List<VersionView> buildVersions(List<Long> templateIds) {
        Map<Long, String> userMap = resolveUserMap(Set.copyOf(versionMapper.selectList(
                        new LambdaQueryWrapper<PromptTemplateVersionEntity>().in(PromptTemplateVersionEntity::getTemplateId, templateIds))
                .stream().map(PromptTemplateVersionEntity::getEditorUserId).collect(Collectors.toSet())));
        return versionMapper.selectList(new LambdaQueryWrapper<PromptTemplateVersionEntity>()
                        .in(PromptTemplateVersionEntity::getTemplateId, templateIds)
                        .orderByAsc(PromptTemplateVersionEntity::getCreatedAt))
                .stream()
                .map(item -> new VersionView(item.getVersionId(), item.getVersionNo(), item.getChangeNote(), item.getPromptContent(),
                        userMap.getOrDefault(item.getEditorUserId(), "未知编辑者"), item.getCreatedAt()))
                .toList();
    }

    private List<ReviewView> buildReviews(List<Long> templateIds) {
        List<TemplateReviewEntity> reviews = reviewMapper.selectList(new LambdaQueryWrapper<TemplateReviewEntity>()
                .in(TemplateReviewEntity::getTemplateId, templateIds)
                .orderByDesc(TemplateReviewEntity::getCreatedAt));
        if (reviews.isEmpty()) {
            return List.of();
        }
        Map<Long, String> userMap = resolveUserMap(reviews.stream().map(TemplateReviewEntity::getUserId).collect(Collectors.toSet()));
        return reviews.stream()
                .map(item -> new ReviewView(item.getReviewId(), userMap.getOrDefault(item.getUserId(), "匿名用户"),
                        item.getScore(), item.getReviewContent(), item.getCreatedAt()))
                .toList();
    }

    private Map<Long, String> resolveUserMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUserEntity::getUserId, SysUserEntity::getUsername));
    }

    private boolean isFree(PromptTemplateEntity entity) {
        return isFree(entity.getPriceType(), entity.getPrice());
    }

    private boolean isFree(String priceType, BigDecimal price) {
        return "FREE".equalsIgnoreCase(priceType) || price.compareTo(BigDecimal.ZERO) == 0;
    }

    private String resolveStatus(String shelfStatus) {
        return "ON_SHELF".equalsIgnoreCase(shelfStatus) ? "已上架" : "已下架";
    }
}
