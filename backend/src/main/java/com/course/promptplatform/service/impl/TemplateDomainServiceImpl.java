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
import com.course.promptplatform.model.ApiRequests.SearchRequest;
import com.course.promptplatform.model.PortalViewModels.ReviewView;
import com.course.promptplatform.model.PortalViewModels.TemplateCardView;
import com.course.promptplatform.model.PortalViewModels.TemplateDetailView;
import com.course.promptplatform.model.PortalViewModels.VersionView;
import com.course.promptplatform.model.query.TemplateQueryRow;
import com.course.promptplatform.service.TemplateDomainService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


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
                new LambdaQueryWrapper<PromptTemplateEntity>().eq(PromptTemplateEntity::getCreatorUserId, userId));
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
