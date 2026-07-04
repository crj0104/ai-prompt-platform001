package com.course.promptplatform.config;

import com.course.promptplatform.common.BusinessException;
import com.course.promptplatform.common.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 从请求头读取当前用户，避免每个接口重复解析 X-User-Id。
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String rawUserId = request.getHeader(USER_ID_HEADER);
        if (rawUserId == null || rawUserId.isBlank()) {
            CurrentUser.setUserId(null);
            return true;
        }
        try {
            CurrentUser.setUserId(Long.valueOf(rawUserId));
            return true;
        } catch (NumberFormatException ex) {
            CurrentUser.clear();
            throw new BusinessException("X-User-Id must be a valid number");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUser.clear();
    }
}
