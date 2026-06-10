package org.example.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.common.ApiResponse;
import org.example.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    @Autowired
    private AuthService authService;


    /**
     * POST /api/auth/login - 学号密码登录
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, Object> body,
                                                   HttpSession session,
                                                   HttpServletResponse response) {
        String studentId = (String) body.get("studentId");
        String password = (String) body.get("password");
        Boolean rememberMe = (Boolean) body.getOrDefault("rememberMe", false);

        // 参数校验
        if (studentId == null || studentId.isBlank() || password == null || password.isBlank()) {
            return ApiResponse.error(400, "学号和密码不能为空");
        }

        // 调用 AuthService 登录
        Map<String, Object> result = authService.login(studentId, password,
                Boolean.TRUE.equals(rememberMe), session, response);

        // 转换为 ApiResponse 格式
        if (Boolean.TRUE.equals(result.get("success"))) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("studentId", result.get("studentId"));
            data.put("studentName", result.get("studentName"));
            data.put("className", result.get("className"));
            data.put("avatarUrl", result.get("avatarUrl"));
            return ApiResponse.ok(data);
        } else {
            return ApiResponse.error(401, (String) result.getOrDefault("message", "学号或密码错误"));
        }
    }

    /**
     * POST /api/auth/logout - 登出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session, HttpServletResponse response) {
        authService.logout(session, response);
        return ApiResponse.ok();
    }

    /**
     * GET /api/auth/check - 检查登录状态
     * 未登录时返回 {loggedIn: false}，不返回 401
     */
    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> check(HttpSession session) {
        Map<String, Object> result = authService.check(session);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("loggedIn", result.getOrDefault("loggedIn", false));
        data.put("studentId", result.get("studentId"));
        data.put("studentName", result.get("studentName"));
        data.put("className", result.get("className"));
        data.put("phoneNumber", result.get("phoneNumber"));
        data.put("avatarUrl", result.get("avatarUrl"));
        return ApiResponse.ok(data);
    }

    /**
     * GET /api/auth/profile - 获取个人信息
     */
    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> getProfile(HttpSession session) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null) {
            return ApiResponse.error(401, "未登录");
        }

        Map<String, Object> profileData = authService.getProfile(studentId);
        if (profileData == null) {
            return ApiResponse.error(404, "学生不存在");
        }

        return ApiResponse.ok(profileData);
    }

    /**
     * PUT /api/auth/profile - 更新个人信息
     */
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestBody Map<String, Object> body,
                                            HttpSession session) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null) {
            return ApiResponse.error(401, "未登录");
        }

        String studentName = (String) body.getOrDefault("studentName", "");
        String className = (String) body.getOrDefault("className", "");
        String phoneNumber = (String) body.getOrDefault("phoneNumber", "");
        String dormBuilding = (String) body.getOrDefault("dormBuilding", "");
        String roomNumber = (String) body.getOrDefault("roomNumber", "");

        Map<String, Object> result = authService.updateProfile(studentId, studentName, className,
                phoneNumber, dormBuilding, roomNumber);
        if (Boolean.TRUE.equals(result.get("success"))) {
            // 更新 session 中的属性
            if (body.containsKey("className")) session.setAttribute("className", className);
            if (body.containsKey("phoneNumber")) session.setAttribute("phoneNumber", phoneNumber);
            if (body.containsKey("studentName")) session.setAttribute("studentName", studentName);
            if (body.containsKey("dormBuilding")) session.setAttribute("dormBuilding", dormBuilding);
            if (body.containsKey("roomNumber")) session.setAttribute("roomNumber", roomNumber);
            return ApiResponse.ok();
        } else {
            return ApiResponse.error(500, (String) result.getOrDefault("message", "更新失败"));
        }
    }
}
