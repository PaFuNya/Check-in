package org.example.service.impl;

import org.example.service.LocationVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * GPS定位验证服务实现
 * 使用Haversine公式计算两点间距离，判断是否在寝室范围内
 */
@Service
public class LocationVerificationServiceImpl implements LocationVerificationService {

    private static final Logger log = LoggerFactory.getLogger(LocationVerificationServiceImpl.class);

    @Value("${app.dorm.latitude:30.73}")
    private double dormLatitude;

    @Value("${app.dorm.longitude:121.03}")
    private double dormLongitude;

    @Value("${app.dorm.radius:500}")
    private double allowedRadius; // 允许范围，单位：米

    @Override
    public boolean verifyLocation(String studentId, double latitude, double longitude) {
        double distance = haversine(dormLatitude, dormLongitude, latitude, longitude);
        log.info("GPS定位验证 - 学号: {}, 学生位置: ({}, {}), 寝室位置: ({}, {}), 距离: {}米, 允许范围: {}米",
                studentId, latitude, longitude, dormLatitude, dormLongitude,
                String.format("%.2f", distance), allowedRadius);
        boolean withinRange = distance <= allowedRadius;
        if (!withinRange) {
            log.warn("GPS定位验证失败 - 学号: {}, 距离寝室{}米，超出{}米范围", studentId,
                    String.format("%.2f", distance), allowedRadius);
        }
        return withinRange;
    }

    /**
     * Haversine公式计算两个GPS坐标之间的距离（单位：米）
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS = 6371000; // 地球半径，单位：米
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }
}
