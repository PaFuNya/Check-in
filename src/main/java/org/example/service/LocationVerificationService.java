package org.example.service;

/**
 * GPS定位验证服务
 * 验证学生签到时的地理位置是否在寝室范围内
 */
public interface LocationVerificationService {
    /**
     * 验证位置是否在寝室范围内
     * @param studentId 学号（用于获取寝室位置）
     * @param latitude 纬度
     * @param longitude 经度
     * @return true=在范围内
     */
    boolean verifyLocation(String studentId, double latitude, double longitude);
}
