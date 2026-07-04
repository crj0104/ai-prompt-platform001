package com.course.promptplatform.common;

/**
 * 保存一次 HTTP 请求中的当前用户编号。
 */
public final class CurrentUser {

    private static final long DEMO_USER_ID = 1L;
    private static final ThreadLocal<Long> USER_ID = ThreadLocal.withInitial(() -> DEMO_USER_ID);

    private CurrentUser() {
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId == null || userId <= 0 ? DEMO_USER_ID : userId);
    }

    public static Long userId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
