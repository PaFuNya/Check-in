package org.example.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body,
                                      HttpSession session,
                                      HttpServletResponse response) {
        String studentId = (String) body.get("studentId");
        String password = (String) body.get("password");
        Boolean rememberMe = (Boolean) body.getOrDefault("rememberMe", false);
        return authService.login(studentId, password, rememberMe, session, response);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpSession session, HttpServletResponse response) {
        authService.logout(session, response);
        return Map.of("success", "true", "message", "已登出");
    }

    @GetMapping("/check")
    public Map<String, Object> check(HttpSession session) {
        return authService.check(session);
    }

    @GetMapping("/profile")
    public Map<String, Object> getProfile(HttpSession session) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null) {
            return Map.of("success", false, "message", "未登录");
        }
        Map<String, Object> profile = authService.getProfile(studentId);
        return profile;
    }

    @PostMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody Map<String, Object> body,
                                              HttpSession session) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null) {
            return Map.of("success", false, "message", "未登录");
        }
        String className = (String) body.getOrDefault("className", "");
        String phoneNumber = (String) body.getOrDefault("phoneNumber", "");
        String avatarUrl = (String) body.getOrDefault("avatarUrl", "");
        Map<String, Object> result = authService.updateProfile(studentId, className, phoneNumber, avatarUrl);
        if (Boolean.TRUE.equals(result.get("success"))) {
            session.setAttribute("className", className);
            session.setAttribute("phoneNumber", phoneNumber);
            session.setAttribute("avatarUrl", avatarUrl);
        }
        return result;
    }
}
