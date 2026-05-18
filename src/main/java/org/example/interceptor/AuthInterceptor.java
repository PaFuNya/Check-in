package org.example.interceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.StudentEntity;
import org.example.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 排除路径: 放行
        if (isExcluded(uri)) {
            return true;
        }

        HttpSession session = request.getSession();

        // 1. 检查 session 是否已登录
        if (session.getAttribute("studentId") != null) {
            return true;
        }

        // 2. session 无登录态, 检查 cookie 中的 remember_token
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("remember_token".equals(cookie.getName())) {
                    String tokenValue = cookie.getValue();
                    if (tokenValue != null && !tokenValue.isBlank()) {
                        StudentEntity student = authService.validateRememberToken(tokenValue);
                        if (student != null) {
                            session.setAttribute("studentId", student.getStudentId());
                            session.setAttribute("studentName", student.getStudentName());
                            log.info("Auto-login via remember token: {}", student.getStudentId());
                            return true;
                        } else {
                            Cookie clearCookie = new Cookie("remember_token", "");
                            clearCookie.setPath("/");
                            clearCookie.setMaxAge(0);
                            response.addCookie(clearCookie);
                        }
                    }
                }
            }
        }

        // 3. 未登录 — 根据请求类型返回不同响应
        if (isApiRequest(uri)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"未登录\"}");
        } else {
            response.sendRedirect("/login");
        }
        return false;
    }

    private boolean isExcluded(String uri) {
        return uri.equals("/auth/login") || uri.equals("/auth/logout") || uri.equals("/login")
                || uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/")
                || uri.startsWith("/static/") || uri.equals("/favicon.ico");
    }

    private boolean isApiRequest(String uri) {
        return uri.startsWith("/ai/") || uri.startsWith("/checkin/") || uri.startsWith("/face/") || uri.equals("/auth/check");
    }
}
