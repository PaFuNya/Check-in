package org.example.service.impl;

import org.example.entity.StudentEntity;
import org.example.repository.StudentRepository;
import org.example.service.BaiduFaceService;
import org.example.service.FaceRecognitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 人脸识别服务实现 - 接入百度AI人脸识别API
 */
@Service
public class DemoFaceRecognitionServiceImpl implements FaceRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(DemoFaceRecognitionServiceImpl.class);

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private BaiduFaceService baiduFaceService;

    @Override
    public boolean verifyFace(String studentId, String faceImageData) {
        log.info("开始人脸验证 - 学号: {}", studentId);

        if (faceImageData == null || faceImageData.isBlank()) {
            log.warn("人脸图片数据为空");
            return false;
        }

        // 第一步：人脸检测
        BaiduFaceService.FaceDetectResult detectResult = baiduFaceService.detectFace(faceImageData);
        if (!detectResult.isSuccess()) {
            log.warn("人脸验证失败[检测] - 学号: {}, 原因: {}", studentId, detectResult.getMessage());
            return false;
        }

        // 第二步：人脸搜索匹配
        BaiduFaceService.FaceSearchResult searchResult = baiduFaceService.searchFace(faceImageData);
        if (!searchResult.isSuccess()) {
            log.warn("人脸验证失败[搜索] - 学号: {}, 原因: {}", studentId, searchResult.getMessage());
            return false;
        }

        // 验证搜索到的userId是否与传入的studentId一致
        if (!studentId.equals(searchResult.getUserId())) {
            log.warn("人脸验证失败[匹配] - 学号: {}, 搜索到的用户: {}", studentId, searchResult.getUserId());
            return false;
        }

        log.info("人脸验证通过 - 学号: {}, 匹配分数: {}", studentId, searchResult.getScore());
        return true;
    }

    @Override
    public boolean registerFace(String studentId, String faceImageData) {
        log.info("注册人脸数据 - 学号: {}", studentId);
        Optional<StudentEntity> optional = studentRepository.findById(studentId);
        if (optional.isEmpty()) {
            log.warn("注册人脸失败：学生不存在, 学号: {}", studentId);
            return false;
        }

        if (faceImageData == null || faceImageData.isBlank()) {
            log.warn("注册人脸失败：图片数据为空");
            return false;
        }

        // 先检测图片中是否有人脸
        BaiduFaceService.FaceDetectResult result = baiduFaceService.detectFace(faceImageData);
        if (!result.isSuccess()) {
            log.warn("注册人脸失败：图片中未检测到有效人脸 - {}", result.getMessage());
            return false;
        }

        // 注册到百度人脸库
        BaiduFaceService.FaceResult addResult = baiduFaceService.addFaceToGroup(studentId, faceImageData);
        if (!addResult.isSuccess()) {
            log.warn("注册人脸失败：百度人脸库注册失败 - {}", addResult.getMessage());
            return false;
        }

        StudentEntity student = optional.get();
        student.setFaceImageUrl("registered");
        studentRepository.save(student);
        log.info("人脸数据注册成功 - 学号: {}", studentId);
        return true;
    }
}
