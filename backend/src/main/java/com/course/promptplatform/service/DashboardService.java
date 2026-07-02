package com.course.promptplatform.service;

import java.util.Map;

public interface DashboardService {

    Map<String, Object> creatorDashboard(Long userId);

    Map<String, Object> adminDashboard();
}
