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
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private RememberTokenRepository rememberTokenRepository;

    @Override
    @Transactional
    public Map<String, Object> login(String studentId, String password, boolean rememberMe,
                                      HttpSession session, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();

        // 查找学生
        Optional<StudentEntity> optionalStudent = studentRepository.findById(studentId);
        if (optionalStudent.isEmpty()) {
            result.put("success", false);
            result.put("message", "学号或密码错误");
            return result;
        }

        StudentEntity student = optionalStudent.get();

        // 验证密码
        if (!passwordEncoder.matches(password, student.getPassword())) {
            result.put("success", false);
            result.put("message", "学号或密码错误");
            return result;
        }

        // 登录成功, 写入 session
        session.setAttribute("studentId", student.getStudentId());
        session.setAttribute("studentName", student.getStudentName());

        // 处理 rememberMe
        if (rememberMe) {
            String token = UUID.randomUUID().toString();
            String tokenHash = sha256(token);

            RememberTokenEntity tokenEntity = new RememberTokenEntity();
            tokenEntity.setStudentId(student.getStudentId());
            tokenEntity.setToken(tokenHash);
            tokenEntity.setExpiresAt(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000));
            rememberTokenRepository.save(tokenEntity);

            Cookie cookie = new Cookie("remember_token", token);
            cookie.setMaxAge(30 * 24 * 60 * 60);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            response.addCookie(cookie);

            // Servlet Cookie API 不支持 setSameSite, 用 addHeader 追加 SameSite
            response.addHeader("Set-Cookie", buildSetCookieHeader(token));
        } else {
            // 未勾选"记住我"时，清除已有的 remember_token cookie 和数据库记录
            rememberTokenRepository.deleteByStudentId(student.getStudentId());
            Cookie clearCookie = new Cookie("remember_token", "");
            clearCookie.setPath("/");
            clearCookie.setMaxAge(0);
            clearCookie.setHttpOnly(true);
            response.addCookie(clearCookie);
            response.addHeader("Set-Cookie", "remember_token=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax");
        }

        result.put("success", true);
        result.put("message", "登录成功");
        result.put("studentId", student.getStudentId());
        return result;
    }

    private String buildSetCookieHeader(String token) {
        return "remember_token=" + token
                + "; Max-Age=" + (30 * 24 * 60 * 60)
                + "; Path=/"
                + "; HttpOnly"
                + "; Secure"
                + "; SameSite=Lax";
    }

    @Override
    @Transactional
    public void logout(HttpSession session, HttpServletResponse response) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId != null) {
            rememberTokenRepository.deleteByStudentId(studentId);
        }

        session.invalidate();

        Cookie clearCookie = new Cookie("remember_token", "");
        clearCookie.setPath("/");
        clearCookie.setMaxAge(0);
        clearCookie.setSecure(true);
        response.addCookie(clearCookie);
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
        }
        return result;
    }

    @Override
    public StudentEntity validateRememberToken(String token) {
        String tokenHash = sha256(token);
        Optional<RememberTokenEntity> optionalToken = rememberTokenRepository.findByToken(tokenHash);
        if (optionalToken.isEmpty()) {
            return null;
        }

        RememberTokenEntity tokenEntity = optionalToken.get();
        if (tokenEntity.getExpiresAt().before(new Date())) {
            rememberTokenRepository.delete(tokenEntity);
            return null;
        }

        Optional<StudentEntity> optionalStudent = studentRepository.findById(tokenEntity.getStudentId());
        return optionalStudent.orElse(null);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
