package org.example.controller;

import org.example.entity.StudentEntity;
import org.example.repository.StudentRepository;
import org.example.service.BaiduFaceService;
import org.example.service.FaceRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/face")
public class FaceRegisterController {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private BaiduFaceService baiduFaceService;

    /**
     * 人脸检测接口 — 多帧活体检测 + 人脸搜索
     */
    @PostMapping("/detect")
    @SuppressWarnings("unchecked")
    public Map<String, Object> detect(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String studentId = (String) session.getAttribute("studentId");

        if (studentId == null || studentId.isBlank()) {
            result.put("success", false);
            result.put("message", "未登录，请先登录");
            result.put("faceVerified", false);
            return result;
        }

        // 多帧活体检测流程
        Object framesObj = body.get("frames");
        if (framesObj instanceof List && !((List<?>) framesObj).isEmpty()) {
            List<String> frames = (List<String>) framesObj;

            // 第一步：多帧活体检测
            BaiduFaceService.LivenessResult livenessResult = baiduFaceService.verifyLivenessMultiFrame(frames);
            if (!livenessResult.isSuccess()) {
                result.put("success", false);
                result.put("message", livenessResult.getMessage());
                result.put("faceVerified", false);
                return result;
            }

            // 第二步：用第一帧做人脸搜索匹配
            BaiduFaceService.FaceSearchResult searchResult = baiduFaceService.searchFace(frames.get(0));
            if (!searchResult.isSuccess()) {
                result.put("success", false);
                result.put("message", searchResult.getMessage());
                result.put("faceVerified", false);
                return result;
            }
            if (!studentId.equals(searchResult.getUserId())) {
                result.put("success", false);
                result.put("message", "人脸匹配失败：与注册人脸不符");
                result.put("faceVerified", false);
                return result;
            }

            result.put("success", true);
            result.put("message", "活体检测通过，人脸匹配成功");
            result.put("faceVerified", true);
            result.put("confidence", searchResult.getScore() / 100.0);
            return result;
        }

        // 单帧回退流程
        String imageData = (String) body.get("imageData");
        if (imageData == null || imageData.isBlank()) {
            result.put("success", false);
            result.put("message", "未提供图片数据");
            result.put("faceVerified", false);
            return result;
        }

        boolean faceVerified = faceRecognitionService.verifyFace(studentId, imageData);
        result.put("success", faceVerified);
        result.put("message", faceVerified ? "人脸验证通过" : "人脸验证失败，与注册人脸不匹配");
        result.put("faceVerified", faceVerified);
        return result;
    }

    /**
     * 人脸注册接口 — 拍照注册
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String studentId = (String) session.getAttribute("studentId");
        String imageData = body.get("imageData");

        if (studentId == null || studentId.isBlank()) {
            result.put("success", false);
            result.put("message", "未登录，请先登录");
            return result;
        }

        if (imageData == null || imageData.isBlank()) {
            result.put("success", false);
            result.put("message", "未提供图片数据");
            return result;
        }

        boolean registered = faceRecognitionService.registerFace(studentId, imageData);
        if (registered) {
            result.put("success", true);
            result.put("message", "人脸注册成功");
        } else {
            result.put("success", false);
            result.put("message", "人脸注册失败，请确保图片中有清晰人脸");
        }
        return result;
    }

    /**
     * 人脸注册状态查询
     */
    @GetMapping("/status")
    public Map<String, Object> status(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String studentId = (String) session.getAttribute("studentId");

        if (studentId == null || studentId.isBlank()) {
            result.put("success", false);
            result.put("message", "未登录");
            result.put("registered", false);
            return result;
        }

        Optional<StudentEntity> optional = studentRepository.findById(studentId);
        if (optional.isEmpty()) {
            result.put("success", false);
            result.put("message", "学生不存在");
            result.put("registered", false);
            return result;
        }

        StudentEntity student = optional.get();
        boolean registered = "registered".equals(student.getFaceImageUrl());

        result.put("success", true);
        result.put("message", registered ? "已注册" : "未注册");
        result.put("registered", registered);
        return result;
    }
}
