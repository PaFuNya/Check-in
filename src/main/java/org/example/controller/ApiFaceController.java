package org.example.controller;

import jakarta.servlet.http.HttpSession;
import org.example.common.ApiResponse;
import org.example.entity.StudentEntity;
import org.example.repository.StudentRepository;
import org.example.service.BaiduFaceService;
import org.example.service.FaceRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/face")
public class ApiFaceController {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private BaiduFaceService baiduFaceService;

    /**
     * POST /api/face/register - 人脸注册
     */
    @PostMapping("/register")
    @SuppressWarnings("unchecked")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, String> body,
                                                      HttpSession session) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null || studentId.isBlank()) {
            return ApiResponse.error(401, "未登录");
        }

        String imageData = body.get("imageData");
        if (imageData == null || imageData.isBlank()) {
            return ApiResponse.error(400, "缺少图片数据");
        }

        boolean registered = faceRecognitionService.registerFace(studentId, imageData);
        if (registered) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("registered", true);
            return ApiResponse.ok(data);
        } else {
            return ApiResponse.error(500, "人脸注册失败，请确保图片中有清晰人脸");
        }
    }

    /**
     * POST /api/face/detect - 人脸检测（多帧活体检测 + 人脸搜索）
     */
    @PostMapping("/detect")
    @SuppressWarnings("unchecked")
    public ApiResponse<Map<String, Object>> detect(@RequestBody Map<String, Object> body,
                                                    HttpSession session) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null || studentId.isBlank()) {
            return ApiResponse.error(401, "未登录");
        }

        // 多帧活体检测流程
        Object framesObj = body.get("frames");
        if (framesObj instanceof List && !((List<?>) framesObj).isEmpty()) {
            List<String> frames = (List<String>) framesObj;

            // 第一步：多帧活体检测
            BaiduFaceService.LivenessResult livenessResult =
                    baiduFaceService.verifyLivenessMultiFrame(frames);
            if (!livenessResult.isSuccess()) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("faceVerified", false);
                data.put("confidence", 0.0);
                data.put("livenessPassed", false);
                return ApiResponse.error(400, livenessResult.getMessage());
            }

            // 第二步：用第一帧做人脸搜索匹配
            BaiduFaceService.FaceSearchResult searchResult =
                    baiduFaceService.searchFace(frames.get(0));
            if (!searchResult.isSuccess()) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("faceVerified", false);
                data.put("confidence", 0.0);
                data.put("livenessPassed", true);
                return ApiResponse.error(400, searchResult.getMessage());
            }

            // 第三步：验证搜索结果 userId 与当前 studentId 一致
            if (!studentId.equals(searchResult.getUserId())) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("faceVerified", false);
                data.put("confidence", searchResult.getScore() / 100.0);
                data.put("livenessPassed", true);
                return ApiResponse.error(400, "人脸匹配失败：与注册人脸不符");
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("faceVerified", true);
            data.put("confidence", searchResult.getScore() / 100.0);
            data.put("livenessPassed", true);
            return ApiResponse.ok(data);
        }

        // 单帧回退流程
        String imageData = (String) body.get("imageData");
        if (imageData == null || imageData.isBlank()) {
            return ApiResponse.error(400, "未提供图片数据");
        }

        boolean faceVerified = faceRecognitionService.verifyFace(studentId, imageData);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("faceVerified", faceVerified);
        data.put("confidence", faceVerified ? 1.0 : 0.0);
        data.put("livenessPassed", faceVerified);

        if (faceVerified) {
            return ApiResponse.ok(data);
        } else {
            return ApiResponse.error(400, "人脸验证失败，与注册人脸不匹配");
        }
    }

    /**
     * GET /api/face/status - 查询人脸注册状态
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(HttpSession session) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null || studentId.isBlank()) {
            return ApiResponse.error(401, "未登录");
        }

        Optional<StudentEntity> optional = studentRepository.findById(studentId);
        if (optional.isEmpty()) {
            return ApiResponse.error(404, "学生不存在");
        }

        StudentEntity student = optional.get();
        boolean registered = "registered".equals(student.getFaceImageUrl());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("registered", registered);
        return ApiResponse.ok(data);
    }
}
