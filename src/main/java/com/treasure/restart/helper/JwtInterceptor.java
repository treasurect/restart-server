package com.treasure.restart.helper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        try {
            String bearer_ = "Bearer ";
            if (authorization == null || authorization.isEmpty()) {
                unauthorized(response,"未登录");
                return false;
            }
            if (!authorization.startsWith(bearer_)) {
                unauthorized(response,"Token格式错误");
                return false;
            }
            String token = authorization.substring(bearer_.length());
            Long userId = JwtUtils.getUserId(token);
            //request.setAttribute("userId", userId);
            UserContext.setUserId(userId);
            return true;
        } catch (Exception e) {
            unauthorized(response,"登录已过期，请重新登录");
            return false;
        }
    }

    private static void unauthorized(HttpServletResponse response,String message){
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
            {
                "code":401,
                "message":"%s",
                "data":null
            }
            """.formatted(message));
        }catch (IOException ignored){}
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        UserContext.clear();
    }
}
