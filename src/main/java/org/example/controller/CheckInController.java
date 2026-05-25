package org.example.controller;

import jakarta.servlet.http.HttpSession;
import org.example.entity.CheckInRecordEntity;
import org.example.entity.StudentEntity;
import org.example.repository.CheckInRecordRepository;
import org.example.repository.StudentRepository;
import org.example.service.FaceRecognitionService;
import org.example.service.LocationVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/checkin")
public class CheckInController {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Autowired
    private LocationVerificationService locationVerificationService;

    @Autowired
    private CheckInRecordRepository checkInRecordRepository;

    @Autowired
    private StudentRepository studentRepository;

    /**
     * 签到验证接口
     * 需要同时通过人脸识别和GPS定位验证
     */
    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        // 从 session 获取 studentId, 不信任请求体中的值
        String studentId = (String) session.getAttribute("studentId");
        String faceImageData = (String) body.get("faceImageData");
        Number latNum = (Number) body.get("latitude");
        Number lngNum = (Number) body.get("longitude");

        // 参数校验
        if (studentId == null) {
            result.put("success", false);
            result.put("message", "未登录");
            result.put("faceVerified", false);
            result.put("locationVerified", false);
            return result;
        }

        if (faceImageData == null || latNum == null || lngNum == null) {
            result.put("success", false);
            result.put("message", "参数不完整，请提供faceImageData、latitude、longitude");
            result.put("faceVerified", false);
            result.put("locationVerified", false);
            return result;
        }

        double latitude = latNum.doubleValue();
        double longitude = lngNum.doubleValue();

        // 人脸识别验证
        boolean faceVerified = faceRecognitionService.verifyFace(studentId, faceImageData);
        // GPS定位验证
        boolean locationVerified = locationVerificationService.verifyLocation(studentId, latitude, longitude);

        if (faceVerified && locationVerified) {
            // 两者都通过，创建签到记录
            Optional<StudentEntity> optional = studentRepository.findById(studentId);
            if (optional.isEmpty()) {
                result.put("success", false);
                result.put("message", "学生不存在");
                result.put("faceVerified", faceVerified);
                result.put("locationVerified", locationVerified);
                return result;
            }

            StudentEntity student = optional.get();
            CheckInRecordEntity record = new CheckInRecordEntity();
            record.setStudentId(studentId);
            record.setStudentName(student.getStudentName());
            record.setRoomNumber(student.getRoomNumber());
            record.setDormBuilding(student.getDormBuilding());
            record.setStatus("已签到");
            record.setLocationInfo(latitude + "," + longitude);
            record.setCheckTime(new Date());
            checkInRecordRepository.save(record);

            result.put("success", true);
            result.put("message", "签到成功");
            result.put("faceVerified", true);
            result.put("locationVerified", true);
        } else {
            StringBuilder msg = new StringBuilder("签到失败：");
            if (!faceVerified) msg.append("人脸识别未通过; ");
            if (!locationVerified) msg.append("GPS定位不在寝室范围内; ");
            result.put("success", false);
            result.put("message", msg.toString().trim());
            result.put("faceVerified", faceVerified);
            result.put("locationVerified", locationVerified);
        }

        return result;
    }
}
