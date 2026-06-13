-- MySQL dump 10.13  Distrib 8.4.9, for Linux (x86_64)
--
-- Host: localhost    Database: checkin_db
-- ------------------------------------------------------
-- Server version	8.4.9

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `chat_history`
--

DROP TABLE IF EXISTS `chat_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `session_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ä¼šè¯id',
  `student_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学生id',
  `role` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色',
  `content` longtext COLLATE utf8mb4_unicode_ci COMMENT 'å†…å®¹',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=213 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='èŠå¤©åŽ†å²';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_history`
--

LOCK TABLES `chat_history` WRITE;
/*!40000 ALTER TABLE `chat_history` DISABLE KEYS */;
INSERT INTO `chat_history` VALUES (209,'0307124223','0307124223','0','今天签到了吗？','2026-06-10 21:11:19','2026-06-10 21:11:19'),(210,'0307124223','0307124223','1','我先查一下聊天历史，看看能否获取用户的学生信息。目前没有获取到你的学生信息，无法直接查询签到记录。请问你能告诉我你的**学号（学生ID）**是多少吗？这样我就可以帮你查今天的签到状态啦！📋','2026-06-10 21:11:26','2026-06-10 21:11:26'),(211,'0307124223','0307124223','0','0307124223','2026-06-10 21:11:37','2026-06-10 21:11:37'),(212,'0307124223','0307124223','1','我需要先获取用户的聊天历史来确定学生的身份信息。看来用户提供了sessionId 0307124223，这个很可能就是学号。我来查询一下签到记录和请假记录。同学你好！👋\n\n我查询了你的信息（学号：0307124223），目前结果显示：\n\n### 📌 签到状态\n- **今日签到：** ❌ 暂未查到签到记录\n\n### 📌 请假记录\n- **请假申请：** ❌ 暂未查到请假记录\n\n看起来你今天**还没有签到**哦～如果有特殊情况没能签到，可以考虑申请请假呢！如果已经签到了但查不到记录，建议联系宿管老师确认一下系统情况。😊','2026-06-10 21:11:48','2026-06-10 21:11:48');
/*!40000 ALTER TABLE `chat_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `check_in_record`
--

DROP TABLE IF EXISTS `check_in_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `check_in_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `student_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学生id',
  `student_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学生姓名',
  `room_number` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '寝室号',
  `dorm_building` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '宿舍楼栋',
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '签到状态',
  `location_info` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ä½ç½®ä¿¡æ¯',
  `check_time` datetime DEFAULT NULL COMMENT 'ç­¾åˆ°æ—¶é—´',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_check_time` (`check_time`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='å®¿èˆç­¾åˆ°è®°å½•';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `check_in_record`
--

LOCK TABLES `check_in_record` WRITE;
/*!40000 ALTER TABLE `check_in_record` DISABLE KEYS */;
INSERT INTO `check_in_record` VALUES (1,'2024001','张三','101','1号楼','已签到','29.991125,122.179299','2026-05-30 23:10:11','2026-05-30 23:10:11','2026-05-31 13:28:41'),(2,'2024001','张三','101','1号楼','已签到','29.991111,122.179258','2026-05-31 01:12:56','2026-05-31 01:12:56','2026-05-31 13:28:41');
/*!40000 ALTER TABLE `check_in_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `leave_request`
--

DROP TABLE IF EXISTS `leave_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `leave_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `student_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学生id',
  `student_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学生姓名',
  `leave_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请假类型',
  `start_time` datetime DEFAULT NULL COMMENT 'å¼€å§‹æ—¶é—´',
  `end_time` datetime DEFAULT NULL COMMENT 'ç»“æŸæ—¶é—´',
  `reason` longtext COLLATE utf8mb4_unicode_ci COMMENT '请假原因',
  `audit_status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核状态',
  `auditor_comment` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核意见',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='è¯·å‡ç”³è¯·';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `leave_request`
--

LOCK TABLES `leave_request` WRITE;
/*!40000 ALTER TABLE `leave_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `leave_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lost_property`
--

DROP TABLE IF EXISTS `lost_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lost_property` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `username` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ¡åˆ°å¤±ç‰©çš„ç”¨æˆ·å§“å',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ¡åˆ°å¤±ç‰©çš„ç”¨æˆ·æ‰‹æœºå·',
  `lost_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤±ç‰©åç§°',
  `lost_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤±ç‰©ç‰¹å¾',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='å¤±ç‰©è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lost_property`
--

LOCK TABLES `lost_property` WRITE;
/*!40000 ALTER TABLE `lost_property` DISABLE KEYS */;
/*!40000 ALTER TABLE `lost_property` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lost_register`
--

DROP TABLE IF EXISTS `lost_register`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lost_register` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `username` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ç”¨æˆ·å§“å',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ç”¨æˆ·æ‰‹æœºå·',
  `lost_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤±ç‰©åç§°',
  `lost_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤±ç‰©ç‰¹å¾',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ä¸¢å¤±ç™»è®°';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lost_register`
--

LOCK TABLES `lost_register` WRITE;
/*!40000 ALTER TABLE `lost_register` DISABLE KEYS */;
/*!40000 ALTER TABLE `lost_register` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `remember_token`
--

DROP TABLE IF EXISTS `remember_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `remember_token` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `created_date` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `updated_date` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `expires_at` datetime(6) DEFAULT NULL COMMENT '过期时间',
  `student_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学生id',
  `token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '令牌',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记住登录令牌';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `remember_token`
--

LOCK TABLES `remember_token` WRITE;
/*!40000 ALTER TABLE `remember_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `remember_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student`
--

DROP TABLE IF EXISTS `student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student` (
  `student_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学生id',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `dorm_building` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '宿舍楼栋',
  `dorm_floor` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `face_image_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '人脸图片URL',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '密码(BCrypt加密)',
  `room_number` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '寝室号',
  `student_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学生姓名',
  `avatar_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `class_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班级',
  `phone_number` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号码',
  PRIMARY KEY (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student`
--

LOCK TABLES `student` WRITE;
/*!40000 ALTER TABLE `student` DISABLE KEYS */;
INSERT INTO `student` VALUES ('0307124223',NULL,'4号楼',NULL,NULL,'$2b$12$rLlJ/Zbyxr8EFJ58tCpRxejKGoM5uORE80Gl6md9ZCgMg0eDuD8iy','4113','朗东成',NULL,'大数据技术2班','15158540312'),('2024001','2026-05-30 18:34:35.000000','4号寝室楼','1楼','registered','$2b$12$nt8huu66upWg3KwPUCHNaOdlMdKvOmL6FBS3/nHHLeJ9cCdyctuGe','4113','张三','/uploads/avatars/2024001_75db005d.png','大数据2401','15158540312'),('2024002','2026-05-30 18:34:35.000000','2号楼',NULL,NULL,'$2b$10$/pgimT.3mZI.CWqkXWHn5ubdv7HNTVCNQDrtdQtPJX.dxhf/b9YXC','202','李四',NULL,NULL,NULL);
/*!40000 ALTER TABLE `student` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'checkin_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-13 16:35:37
