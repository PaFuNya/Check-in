package org.example.controller;

import jakarta.servlet.http.HttpSession;
import org.example.common.ApiResponse;
import org.example.entity.CheckInRecordEntity;
import org.example.entity.StudentEntity;
import org.example.repository.CheckInRecordRepository;
import org.example.repository.StudentRepository;
import org.example.service.FaceRecognitionService;
import org.example.service.LocationVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/checkin")
public class ApiCheckInController {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Autowired
    private LocationVerificationService locationVerificationService;

    @Autowired
    private CheckInRecordRepository checkInRecordRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * GET /api/checkin/status - 查询今日签到状态
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(HttpSession session) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null) {
            return ApiResponse.error(401, "未登录");
        }

        // 获取今天零点时间
        LocalDate today = LocalDate.now();
        Date todayStart = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<CheckInRecordEntity> todayRecords =
                checkInRecordRepository.findByStudentIdAndCheckTimeAfter(studentId, todayStart);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("checkedIn", !todayRecords.isEmpty());
        data.put("count", todayRecords.size());
        return ApiResponse.ok(data);
    }

    /**
     * POST /api/checkin/verify - 签到验证（人脸+位置）
     */
    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verify(@RequestBody Map<String, Object> body,
                                                    HttpSession session) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null) {
            return ApiResponse.error(401, "未登录");
        }

        // Redis 签到防重检查
        String todayKey = "checkin:today:" + studentId + ":" + new SimpleDateFormat("yyyyMMdd").format(new Date());
        if (Boolean.TRUE.equals(redisTemplate.hasKey(todayKey))) {
            return ApiResponse.error(400, "今日已签到，请勿重复签到");
        }

        String faceImageData = (String) body.get("faceImageData");
        Number latNum = (Number) body.get("latitude");
        Number lngNum = (Number) body.get("longitude");

        // 参数校验
        if (faceImageData == null || faceImageData.isBlank()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("faceVerified", false);
            data.put("locationVerified", false);
            data.put("checkedIn", false);
            return ApiResponse.error(400, "参数不完整：faceImageData 不能为空");
        }
        if (latNum == null || lngNum == null) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("faceVerified", false);
            data.put("locationVerified", false);
            data.put("checkedIn", false);
            return ApiResponse.error(400, "参数不完整：latitude/longitude 不能为空");
        }

        double latitude = latNum.doubleValue();
        double longitude = lngNum.doubleValue();

        // 人脸识别验证
        boolean faceVerified = faceRecognitionService.verifyFace(studentId, faceImageData);
        // GPS定位验证
        boolean locationVerified = locationVerificationService.verifyLocation(studentId, latitude, longitude);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("faceVerified", faceVerified);
        data.put("locationVerified", locationVerified);

        if (faceVerified && locationVerified) {
            // 两者都通过，创建签到记录
            Optional<StudentEntity> optional = studentRepository.findById(studentId);
            if (optional.isEmpty()) {
                data.put("checkedIn", false);
                return ApiResponse.error(404, "学生不存在");
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

            // Redis 记录今日已签到，23小时过期
            redisTemplate.opsForValue().set(todayKey, "1", Duration.ofHours(23));

            data.put("checkedIn", true);
            return ApiResponse.ok(data);
        } else {
            data.put("checkedIn", false);
            StringBuilder msg = new StringBuilder("签到失败：");
            if (!faceVerified) msg.append("人脸识别未通过; ");
            if (!locationVerified) msg.append("GPS不在范围内");
            return ApiResponse.error(400, msg.toString().trim());
        }
    }

    /**
     * GET /api/checkin/records - 查询签到记录（分页）
     */
    @GetMapping("/records")
    public ApiResponse<Map<String, Object>> records(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null) {
            return ApiResponse.error(401, "未登录");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "checkTime"));
        Page<CheckInRecordEntity> pageResult = checkInRecordRepository.findByStudentId(studentId, pageable);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", pageResult.getContent());
        data.put("totalPages", pageResult.getTotalPages());
        data.put("totalElements", pageResult.getTotalElements());
        data.put("currentPage", pageResult.getNumber());
        data.put("size", pageResult.getSize());
        return ApiResponse.ok(data);
    }
}
