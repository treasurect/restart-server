package com.treasure.restart.helper;

import com.treasure.restart.base.BaseResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtils jwtUtils;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        try {
            String bearer_ = "Bearer ";
            if (authorization == null || authorization.isEmpty()) {
                return unauthorized(response, "未登录");
            }
            if (!authorization.startsWith(bearer_)) {
                return unauthorized(response, "Token格式错误");
            }
            String token = authorization.substring(bearer_.length());
            Long userId = jwtUtils.getUserId(token);
            //request.setAttribute("userId", userId);
            UserContext.setUserId(userId);
            return true;
        } catch (Exception e) {
            return unauthorized(response, "登录已过期，请重新登录");
        }
    }

    private boolean unauthorized(HttpServletResponse response, String message) {
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            BaseResponse<?> result = BaseResponse.error(401, message);
            response.getWriter().write(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        UserContext.clear();
    }
}
