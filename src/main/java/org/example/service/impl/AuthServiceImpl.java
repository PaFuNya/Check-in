package org.example.service.impl;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.RememberTokenEntity;
import org.example.entity.StudentEntity;
import org.example.repository.RememberTokenRepository;
import org.example.repository.StudentRepository;
import org.example.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private RememberTokenRepository rememberTokenRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Map<String, Object> login(String studentId, String password, boolean rememberMe,
                                      HttpSession session, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();

        // 查找学生
        var optional = studentRepository.findById(studentId);
        if (optional.isEmpty()) {
            result.put("success", false);
            result.put("message", "学号或密码错误");
            result.put("studentId", studentId);
            return result;
        }

        StudentEntity student = optional.get();

        // 验证密码
        if (!passwordEncoder.matches(password, student.getPassword())) {
            result.put("success", false);
            result.put("message", "学号或密码错误");
            result.put("studentId", studentId);
            return result;
        }

        // 登录成功: 设置 session
        session.setAttribute("studentId", student.getStudentId());
        session.setAttribute("studentName", student.getStudentName());
        session.setAttribute("dormBuilding", student.getDormBuilding());
        session.setAttribute("roomNumber", student.getRoomNumber());
        session.setAttribute("faceRegistered", "registered".equals(student.getFaceImageUrl()));

        // 如果 rememberMe, 生成 token 并设置 cookie
        if (rememberMe) {
            String token = UUID.randomUUID().toString();

            RememberTokenEntity tokenEntity = new RememberTokenEntity();
            tokenEntity.setStudentId(studentId);
            tokenEntity.setToken(token);
            tokenEntity.setExpiresAt(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000));
            rememberTokenRepository.save(tokenEntity);

            Cookie cookie = new Cookie("remember_token", token);
            cookie.setMaxAge(30 * 24 * 60 * 60);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        }

        result.put("success", true);
        result.put("message", "登录成功");
        result.put("studentId", studentId);
        result.put("studentName", student.getStudentName());
        result.put("dormBuilding", student.getDormBuilding());
        result.put("roomNumber", student.getRoomNumber());
        result.put("faceRegistered", "registered".equals(student.getFaceImageUrl()));
        return result;
    }

    @Override
    public void logout(HttpSession session, HttpServletResponse response) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId != null) {
            rememberTokenRepository.deleteByStudentId(studentId);
        }
        session.invalidate();

        // 清除 remember_token cookie
        Cookie cookie = new Cookie("remember_token", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        // 清除 JSESSIONID cookie（确保 session 彻底失效）
        Cookie sessionCookie = new Cookie("JSESSIONID", "");
        sessionCookie.setMaxAge(0);
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);
    }

    @Override
    public Map<String, Object> check(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String studentId = (String) session.getAttribute("studentId");
        String studentName = (String) session.getAttribute("studentName");

        if (studentId == null) {
            result.put("loggedIn", false);
            result.put("studentId", null);
            result.put("studentName", null);
        } else {
            result.put("loggedIn", true);
            result.put("studentId", studentId);
            result.put("studentName", studentName);
            result.put("dormBuilding", session.getAttribute("dormBuilding"));
            result.put("roomNumber", session.getAttribute("roomNumber"));
            result.put("faceRegistered", session.getAttribute("faceRegistered"));
        }
        return result;
    }

    @Override
    public StudentEntity validateRememberToken(String token) {
        var optional = rememberTokenRepository.findByToken(token);
        if (optional.isEmpty()) {
            return null;
        }

        RememberTokenEntity tokenEntity = optional.get();

        // 检查是否过期
        if (tokenEntity.getExpiresAt().before(new Date())) {
            rememberTokenRepository.delete(tokenEntity);
            return null;
        }

        // 查找学生
        return studentRepository.findById(tokenEntity.getStudentId()).orElse(null);
    }

    @Override
    public Map<String, Object> getProfile(String studentId) {
        return studentRepository.findById(studentId).map(student -> {
            Map<String, Object> profile = new HashMap<>();
            profile.put("studentId", student.getStudentId());
            profile.put("studentName", student.getStudentName());
            profile.put("dormBuilding", student.getDormBuilding());
            profile.put("roomNumber", student.getRoomNumber());
            profile.put("faceImageUrl", student.getFaceImageUrl());
            profile.put("className", student.getClassName());
            profile.put("phoneNumber", student.getPhoneNumber());
            return profile;
        }).orElse(null);
    }

    @Override
    public Map<String, Object> updateProfile(String studentId, String studentName, String className,
                                              String phoneNumber, String dormBuilding, String roomNumber) {
        Map<String, Object> result = new HashMap<>();
        var optional = studentRepository.findById(studentId);
        if (optional.isEmpty()) {
            result.put("success", false);
            result.put("message", "学生不存在");
            return result;
        }
        StudentEntity student = optional.get();
        if (studentName != null && !studentName.isBlank()) {
            student.setStudentName(studentName);
        }
        if (className != null) {
            student.setClassName(className);
        }
        if (phoneNumber != null) {
            student.setPhoneNumber(phoneNumber);
        }
        if (dormBuilding != null) {
            student.setDormBuilding(dormBuilding);
        }
        if (roomNumber != null) {
            student.setRoomNumber(roomNumber);
        }
        studentRepository.save(student);
        result.put("success", true);
        result.put("message", "更新成功");
        return result;
    }
}
