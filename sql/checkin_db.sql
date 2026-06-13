-- ============================================================
-- 校园寝室自助签到系统 - 数据库初始化脚本
-- Database: checkin_db
-- MySQL 8.x | utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `checkin_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `checkin_db`;

-- -----------------------------------------------------------
-- 学生表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student` (
  `student_id`     varchar(255) NOT NULL COMMENT '学号（主键）',
  `student_name`   varchar(255) DEFAULT NULL COMMENT '学生姓名',
  `password`       varchar(255) DEFAULT NULL COMMENT '密码（BCrypt加密）',
  `class_name`     varchar(255) DEFAULT NULL COMMENT '班级',
  `phone_number`   varchar(255) DEFAULT NULL COMMENT '手机号码',
  `dorm_building`  varchar(255) DEFAULT NULL COMMENT '宿舍楼栋',
  `dorm_floor`     varchar(255) DEFAULT NULL COMMENT '楼层',
  `room_number`    varchar(255) DEFAULT NULL COMMENT '寝室号',
  `avatar_url`     varchar(255) DEFAULT NULL COMMENT '头像URL',
  `face_image_url` varchar(255) DEFAULT NULL COMMENT '人脸注册状态',
  `created_at`     datetime(6)  DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生信息表';

-- -----------------------------------------------------------
-- 签到记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `check_in_record`;
CREATE TABLE `check_in_record` (
  `id`            bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`    varchar(255) DEFAULT NULL COMMENT '学号',
  `student_name`  varchar(255) DEFAULT NULL COMMENT '学生姓名',
  `room_number`   varchar(255) DEFAULT NULL COMMENT '寝室号',
  `dorm_building` varchar(255) DEFAULT NULL COMMENT '宿舍楼栋',
  `status`        varchar(255) DEFAULT NULL COMMENT '签到状态',
  `location_info` varchar(255) DEFAULT NULL COMMENT 'GPS定位信息',
  `check_time`    datetime     DEFAULT NULL COMMENT '签到时间',
  `created_date`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_date`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_check_time` (`check_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宿舍签到记录表';

-- -----------------------------------------------------------
-- 请假申请表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `leave_request`;
CREATE TABLE `leave_request` (
  `id`             bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`     varchar(255) DEFAULT NULL COMMENT '学号',
  `student_name`   varchar(255) DEFAULT NULL COMMENT '学生姓名',
  `leave_type`     varchar(255) DEFAULT NULL COMMENT '请假类型',
  `start_time`     datetime     DEFAULT NULL COMMENT '开始时间',
  `end_time`       datetime     DEFAULT NULL COMMENT '结束时间',
  `reason`         longtext     DEFAULT NULL COMMENT '请假原因',
  `audit_status`   varchar(255) DEFAULT NULL COMMENT '审核状态',
  `auditor_comment` varchar(255) DEFAULT NULL COMMENT '审核意见',
  `created_date`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_date`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假申请表';

-- -----------------------------------------------------------
-- AI 聊天记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `chat_history`;
CREATE TABLE `chat_history` (
  `id`           bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`   varchar(255) DEFAULT NULL COMMENT '会话ID',
  `student_id`   varchar(255) DEFAULT NULL COMMENT '学号',
  `role`         varchar(255) DEFAULT NULL COMMENT '角色（0=用户, 1=AI）',
  `content`      longtext     DEFAULT NULL COMMENT '消息内容',
  `created_date` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_date` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天记录表';

-- -----------------------------------------------------------
-- 记住登录令牌表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `remember_token`;
CREATE TABLE `remember_token` (
  `id`           bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`   varchar(255) DEFAULT NULL COMMENT '学号',
  `token`        varchar(255) DEFAULT NULL COMMENT 'Remember-Me令牌',
  `expires_at`   datetime(6)  DEFAULT NULL COMMENT '过期时间',
  `created_date` datetime(6)  DEFAULT NULL COMMENT '创建时间',
  `updated_date` datetime(6)  DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记住登录令牌表';

-- -----------------------------------------------------------
-- 插入测试数据（可选）
-- -----------------------------------------------------------
-- 密码均为 BCrypt 加密的 "123456"
INSERT INTO `student` (`student_id`, `student_name`, `password`, `class_name`, `dorm_building`, `room_number`)
VALUES
  ('2024001', '张三', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '大数据2401', '1号楼', '101'),
  ('2024002', '李四', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '大数据2401', '2号楼', '202'),
  ('2024003', '王五', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '大数据2402', '3号楼', '303');
