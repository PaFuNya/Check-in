package org.example.service;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.entity.StudentEntity;

import java.util.Map;

public interface AuthService {

    /**
     * 登录验证
     * @param studentId 学号
     * @param password 密码(明文)
     * @param rememberMe 是否记住登录
     * @param session HTTP Session
     * @param response HTTP Response (用于设置 cookie)
     * @return Map {success: boolean, message: String, studentId: String}
     */
    Map<String, Object> login(String studentId, String password, boolean rememberMe,
                               HttpSession session, HttpServletResponse response);

    /**
     * 登出: 清除 session 和 remember-token cookie, 删除 DB 中的 token
     * @param session HTTP Session
     * @param response HTTP Response
     */
    void logout(HttpSession session, HttpServletResponse response);

    /**
     * 检查登录状态 (供 GET /auth/check 调用)
     * @param session HTTP Session
     * @return Map {loggedIn: boolean, studentId: String, studentName: String, className: String, phoneNumber: String, avatarUrl: String}
     */
    Map<String, Object> check(HttpSession session);

    /**
     * 获取用户个人信息
     * @param studentId 学号
     * @return Map 包含完整的学生信息
     */
    Map<String, Object> getProfile(String studentId);

    /**
     * 更新用户个人信息
     * @param studentId 学号
     * @param className 班级
     * @param phoneNumber 手机号码
     * @param avatarUrl 头像URL
     * @return Map {success: boolean, message: String}
     */
    Map<String, Object> updateProfile(String studentId, String className, String phoneNumber, String avatarUrl);

    /**
     * 根据 remember_token cookie 值验证并获取学生信息 (供拦截器调用)
     * @param token UUID 字符串
     * @return StudentEntity 或 null
     */
    StudentEntity validateRememberToken(String token);
}
