package com.course.promptplatform.service;

import com.course.promptplatform.model.ApiRequests.LoginRequest;
import com.course.promptplatform.model.ApiRequests.RegisterRequest;
import com.course.promptplatform.model.PortalViewModels.UserProfileView;
import java.util.Map;

public interface UserCenterService {

    Map<String, Object> register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);

    UserProfileView getProfile(Long userId);
}
