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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession();

        // 1. 检查 session 是否已登录
        if (session.getAttribute("studentId") != null) {
            return true;  // 已登录, 放行
        }

        // 2. session 无登录态, 检查 cookie 中的 remember_token
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("remember_token".equals(cookie.getName())) {
                    String tokenValue = cookie.getValue();
                    if (tokenValue != null && !tokenValue.isBlank()) {
                        // 3. 验证 token
                        StudentEntity student = authService.validateRememberToken(tokenValue);
                        if (student != null) {
                            // 4. 自动登录: 刷新 session
                            session.setAttribute("studentId", student.getStudentId());
                            session.setAttribute("studentName", student.getStudentName());
                            log.info("Auto-login via remember token: {}", student.getStudentId());
                            return true;
                        } else {
                            // token 无效或过期, 清除 cookie
                            Cookie clearCookie = new Cookie("remember_token", "");
                            clearCookie.setPath("/");
                            clearCookie.setMaxAge(0);
                            response.addCookie(clearCookie);
                        }
                    }
                }
            }
        }

        // 5. 未登录, 但放行请求 (由 Controller 决定是否重定向)
        return true;
    }
}
