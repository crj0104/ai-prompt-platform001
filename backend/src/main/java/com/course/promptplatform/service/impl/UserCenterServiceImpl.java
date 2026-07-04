package com.course.promptplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.course.promptplatform.entity.SysUserEntity;
import com.course.promptplatform.entity.PromptTemplateEntity;
import com.course.promptplatform.mapper.PromptTemplateMapper;
import com.course.promptplatform.mapper.SysUserMapper;
import com.course.promptplatform.model.ApiRequests.LoginRequest;
import com.course.promptplatform.model.ApiRequests.RegisterRequest;
import com.course.promptplatform.model.PortalViewModels.UserProfileView;
import com.course.promptplatform.service.TemplateDomainService;
import com.course.promptplatform.service.TradeService;
import com.course.promptplatform.service.UserCenterService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCenterServiceImpl implements UserCenterService {

    private final SysUserMapper sysUserMapper;
    private final PromptTemplateMapper promptTemplateMapper;
    private final TemplateDomainService templateDomainService;
    private final TradeService tradeService;

    @Override
    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        if (existsBy(SysUserEntity::getUsername, request.getUsername())) {
            return Map.of("success", false, "message", "Username already exists");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank() && existsBy(SysUserEntity::getPhone, request.getPhone())) {
            return Map.of("success", false, "message", "Phone already exists");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank() && existsBy(SysUserEntity::getEmail, request.getEmail())) {
            return Map.of("success", false, "message", "Email already exists");
        }

        SysUserEntity user = new SysUserEntity();
        user.setUsername(request.getUsername());
        user.setPasswordHash(hashPassword(request.getPassword()));
        user.setPhone(trimToNull(request.getPhone()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setCreatorScore(0);
        user.setCreatorLevel(null);
        user.setBalance(new BigDecimal("0.00"));
        user.setUserStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.insert(user);
        return Map.of("success", true, "message", "Registration successful", "userId", user.getUserId());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        SysUserEntity user = findByAccount(request.getAccount());
        if (user == null) {
            return Map.of("success", false, "message", "User not found");
        }
        if (!passwordMatches(request.getPassword(), user.getPasswordHash())) {
            return Map.of("success", false, "message", "Invalid password");
        }
        String role = resolveRole(user);
        return Map.of("success", true, "message", "Login successful",
                "account", request.getAccount(),
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "role", role);
    }

    private String resolveRole(SysUserEntity user) {
        if ("admin".equals(user.getUsername())) return "ADMIN";
        Long templateCount = promptTemplateMapper.selectCount(new LambdaQueryWrapper<PromptTemplateEntity>()
                .eq(PromptTemplateEntity::getCreatorUserId, user.getUserId()));
        if (templateCount != null && templateCount > 0) return "CREATOR";
        if (user.getCreatorLevel() != null) return "CREATOR";
        return "USER";
    }

    @Override
    public Map<String, Object> upgradeToCreator(Long userId) {
        SysUserEntity user = sysUserMapper.selectById(userId);
        if (user == null) return Map.of("success", false, "message", "User not found");
        user.setCreatorLevel("A级创作者");
        if (user.getCreatorScore() == null || user.getCreatorScore() < 1000) {
            user.setCreatorScore(1000);
        }
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return Map.of("success", true,
                "message", "已开通创作者身份",
                "role", "CREATOR",
                "userId", user.getUserId(),
                "username", user.getUsername());
    }

    @Override
    public UserProfileView getProfile(Long userId) {
        SysUserEntity user = sysUserMapper.selectById(userId);
        if (user == null) {
            return UserProfileView.builder()
                    .userId(userId)
                    .username("Unknown user")
                    .balance(BigDecimal.ZERO)
                    .publishedTemplates(java.util.List.of())
                    .favorites(java.util.List.of())
                    .orders(java.util.List.of())
                    .usageLogs(java.util.List.of())
                    .balanceLogs(java.util.List.of())
                    .build();
        }
        return UserProfileView.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .email(user.getEmail())
                .creatorLevel(user.getCreatorLevel())
                .balance(user.getBalance())
                .publishedTemplates(templateDomainService.publishedTemplates(userId))
                .favorites(templateDomainService.favoriteTemplates(userId))
                .orders(tradeService.listOrders(userId))
                .usageLogs(tradeService.listUsageLogs(userId))
                .balanceLogs(tradeService.listBalanceLogs(userId))
                .build();
    }

    private SysUserEntity findByAccount(String account) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .and(wrapper -> wrapper.eq(SysUserEntity::getEmail, account)
                        .or()
                        .eq(SysUserEntity::getPhone, account)
                        .or()
                        .eq(SysUserEntity::getUsername, account))
                .last("LIMIT 1"));
    }

    private boolean passwordMatches(String rawPassword, String storedHash) {
        return storedHash != null && (storedHash.equals(rawPassword) || storedHash.equals(hashPassword(rawPassword)));
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private <T> boolean existsBy(com.baomidou.mybatisplus.core.toolkit.support.SFunction<SysUserEntity, T> column, T value) {
        return sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>().eq(column, value)) > 0;
    }
}
