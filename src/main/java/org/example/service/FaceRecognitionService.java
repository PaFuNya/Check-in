package org.example.service;

/**
 * 人脸识别服务接口
 * 预留第三方人脸识别API接口，接入后可直接调用
 */
public interface FaceRecognitionService {
    /**
     * 验证人脸是否匹配
     * @param studentId 学号
     * @param faceImageData Base64编码的人脸图片数据
     * @return true=匹配成功, false=匹配失败
     */
    boolean verifyFace(String studentId, String faceImageData);

    /**
     * 注册学生人脸数据
     * @param studentId 学号
     * @param faceImageData Base64编码的人脸图片数据
     * @return true=注册成功
     */
    boolean registerFace(String studentId, String faceImageData);
}
