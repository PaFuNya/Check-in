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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession();

        // 1. Check session
        if (session.getAttribute("studentId") != null) {
            return true;
        }

        // 2. Check remember_token cookie
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
                            session.setAttribute("dormBuilding", student.getDormBuilding());
                            session.setAttribute("roomNumber", student.getRoomNumber());
                            session.setAttribute("faceRegistered", "registered".equals(student.getFaceImageUrl()));
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

        // 3. Not authenticated — determine response by request type
        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/api/")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录，请先登录\"}");
            return false;
        }

        response.sendRedirect("/login");
        return false;
    }
}
