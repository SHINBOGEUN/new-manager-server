-- MariaDB dump 10.19  Distrib 10.11.2-MariaDB, for Win64 (AMD64)
--
-- Host: 192.168.10.14    Database: dcim_new
-- ------------------------------------------------------
-- Server version	11.8.2-MariaDB-ubu2404

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `code_group`
--

DROP TABLE IF EXISTS `code_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `code_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '코드 그룹 ID',
  `group_key` varchar(100) NOT NULL COMMENT '그룹 키 (예: DEVICE_TYPE)',
  `group_name` varchar(255) NOT NULL COMMENT '그룹 표시명',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_group_group_key` (`group_key`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공통 코드 그룹';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `code_group`
--

LOCK TABLES `code_group` WRITE;
/*!40000 ALTER TABLE `code_group` DISABLE KEYS */;
INSERT INTO `code_group` VALUES
(2,'MODEL_TYPE','모델 유형','2026-07-01 06:10:38.583321','2026-07-01 06:10:38.583321'),
(3,'LOCATION_TYPE','장소 유형','2026-07-01 06:10:51.261363','2026-07-01 06:10:51.261363'),
(4,'PROTOCOL_TYPE','통신 유형','2026-07-06 00:35:56.320065','2026-07-06 00:36:02.198047'),
(5,'DEVICE_PAGE','Device Page','2026-08-14 05:10:02.731326','2026-08-14 05:10:02.731326'),
(6,'LOCATION_PATH','Location Path','2026-08-27 06:03:49.096222','2026-08-27 06:03:49.096222');
/*!40000 ALTER TABLE `code_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `collection_task`
--

DROP TABLE IF EXISTS `collection_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `collection_task` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Task ID',
  `name` varchar(100) NOT NULL COMMENT 'Task 이름',
  `model_id` int(11) NOT NULL COMMENT 'device_model.id',
  `script_type_id` int(11) NOT NULL COMMENT 'common_code.id (PROTOCOL_TYPE)',
  `active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '활성 여부',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection_task_model_script` (`model_id`,`script_type_id`),
  KEY `idx_collection_task_script_type_id` (`script_type_id`),
  KEY `idx_collection_task_active` (`active`),
  CONSTRAINT `fk_collection_task_model_id` FOREIGN KEY (`model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_collection_task_script_type_id` FOREIGN KEY (`script_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `chk_collection_task_active` CHECK (`active` in (0,1))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모델+프로토콜당 수집 Task 1개';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `collection_task`
--

LOCK TABLES `collection_task` WRITE;
/*!40000 ALTER TABLE `collection_task` DISABLE KEYS */;
INSERT INTO `collection_task` VALUES
(1,'PDU-1P SNMP 수집',9,9,1,'2026-08-19 07:24:04.688702','2026-08-19 07:24:04.688702'),
(2,'PDU-1P Raritan SNMP 수집',10,9,1,'2026-08-20 02:25:45.744719','2026-08-20 02:25:45.744719'),
(3,'PDU-3P Raritan SNMP 수집',3,9,1,'2026-08-20 04:20:27.743621','2026-08-20 05:12:04.013067');
/*!40000 ALTER TABLE `collection_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `collection_task_device`
--

DROP TABLE IF EXISTS `collection_task_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `collection_task_device` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
  `group_id` int(11) NOT NULL COMMENT 'collection_task_group.id',
  `device_id` int(11) NOT NULL COMMENT 'devices.id',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection_task_device_group_device` (`group_id`,`device_id`),
  KEY `idx_collection_task_device_device_id` (`device_id`),
  CONSTRAINT `fk_collection_task_device_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_collection_task_device_group_id` FOREIGN KEY (`group_id`) REFERENCES `collection_task_group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주기 그룹에 속한 장비. 한 Task 안에서 device는 그룹 1개만';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `collection_task_device`
--

LOCK TABLES `collection_task_device` WRITE;
/*!40000 ALTER TABLE `collection_task_device` DISABLE KEYS */;
INSERT INTO `collection_task_device` VALUES
(1,1,9,'2026-08-19 07:24:04.696782','2026-08-19 07:24:04.696782'),
(2,2,10,'2026-08-20 02:25:45.757502','2026-08-20 02:25:45.757502'),
(3,2,11,'2026-08-20 02:25:45.757502','2026-08-20 02:25:45.757502'),
(9,3,17,'2026-08-20 04:20:27.753642','2026-08-20 04:20:27.753642'),
(14,4,12,'2026-08-20 04:57:22.331691','2026-08-20 04:57:22.331691'),
(15,4,13,'2026-08-20 04:57:22.337202','2026-08-20 04:57:22.337202'),
(16,3,18,'2026-08-26 06:49:48.568903','2026-08-26 06:49:48.568903');
/*!40000 ALTER TABLE `collection_task_device` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `collection_task_group`
--

DROP TABLE IF EXISTS `collection_task_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `collection_task_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '그룹 ID',
  `task_id` int(11) NOT NULL COMMENT 'collection_task.id',
  `name` varchar(100) NOT NULL COMMENT '그룹 이름',
  `cron_expression` varchar(100) NOT NULL COMMENT '수집 주기 cron',
  `generated_spec` longtext DEFAULT NULL COMMENT '그룹 수집 JSON spec',
  `collector_job_id` varchar(100) DEFAULT NULL COMMENT 'collector job ID',
  `active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '그룹 활성 여부',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection_task_group_task_cron` (`task_id`,`cron_expression`),
  UNIQUE KEY `uk_collection_task_group_collector_job_id` (`collector_job_id`),
  KEY `idx_collection_task_group_task_id` (`task_id`),
  CONSTRAINT `fk_collection_task_group_task_id` FOREIGN KEY (`task_id`) REFERENCES `collection_task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_collection_task_group_active` CHECK (`active` in (0,1))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Task 안 주기 그룹 (1분/5분 등)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `collection_task_group`
--

LOCK TABLES `collection_task_group` WRITE;
/*!40000 ALTER TABLE `collection_task_group` DISABLE KEYS */;
INSERT INTO `collection_task_group` VALUES
(1,1,'기본 그룹','0 */1 * * * *','{\"taskId\":1,\"groupId\":1,\"modelId\":9,\"protocol\":\"snmp\",\"cronExpression\":\"0 */1 * * * *\",\"community\":\"public\",\"timeoutMs\":2000,\"retries\":1,\"maxConcurrency\":10,\"oids\":[{\"name\":\"V\",\"template\":\"1.3.6.1.4.1.6375.1.1.0\",\"requiresInstance\":false},{\"name\":\"status\",\"template\":\"1.3.6.1.4.1.6375.1.2.0\",\"requiresInstance\":false},{\"name\":\"Hz\",\"template\":\"1.3.6.1.4.1.6375.1.3.0\",\"requiresInstance\":false},{\"name\":\"flag\",\"template\":\"1.3.6.1.4.1.6375.1.4.0\",\"requiresInstance\":false},{\"name\":\"reserved5\",\"template\":\"1.3.6.1.4.1.6375.1.5.0\",\"requiresInstance\":false},{\"name\":\"reserved6\",\"template\":\"1.3.6.1.4.1.6375.1.6.0\",\"requiresInstance\":false},{\"name\":\"reserved7\",\"template\":\"1.3.6.1.4.1.6375.1.7.0\",\"requiresInstance\":false},{\"name\":\"W\",\"template\":\"1.3.6.1.4.1.6375.1.8.0\",\"requiresInstance\":false},{\"name\":\"reserved9\",\"template\":\"1.3.6.1.4.1.6375.1.9.0\",\"requiresInstance\":false}],\"targets\":[{\"deviceId\":9,\"host\":\"192.168.14.114\",\"port\":161,\"instanceId\":null}],\"skipped\":[]}','787056d0-b038-48a4-b0c5-549867f3054a',1,'2026-08-19 07:24:04.692768','2026-08-25 07:47:33.774463'),
(2,2,'1분 그룹','0 */1 * * * *','{\"taskId\":2,\"groupId\":2,\"modelId\":10,\"protocol\":\"snmp\",\"cronExpression\":\"0 */1 * * * *\",\"community\":\"public\",\"timeoutMs\":2000,\"retries\":1,\"maxConcurrency\":10,\"oids\":[{\"name\":\"AMP\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.1\",\"requiresInstance\":false},{\"name\":\"PF\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.7\",\"requiresInstance\":false},{\"name\":\"TOTAL_WT\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.5\",\"requiresInstance\":false},{\"name\":\"TOTAL_KWH\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.8\",\"requiresInstance\":false}],\"targets\":[{\"deviceId\":10,\"host\":\"14.42.43.207\",\"port\":30263,\"instanceId\":null},{\"deviceId\":11,\"host\":\"14.42.43.207\",\"port\":30264,\"instanceId\":null}],\"skipped\":[]}','c858e1aa-1624-4aac-8ff2-4cad73dad8ee',1,'2026-08-20 02:25:45.749603','2026-08-26 06:47:26.003513'),
(3,3,'5분 그룹','0 */5 * * * *','{\"taskId\":3,\"groupId\":3,\"modelId\":3,\"protocol\":\"snmp\",\"cronExpression\":\"0 */5 * * * *\",\"community\":\"public\",\"timeoutMs\":2000,\"retries\":1,\"maxConcurrency\":10,\"oids\":[{\"name\":\"TOTAL_WT\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.5\",\"requiresInstance\":false},{\"name\":\"AMP\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.1\",\"requiresInstance\":false},{\"name\":\"PF\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.7\",\"requiresInstance\":false},{\"name\":\"TOTAL_KWH\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.8\",\"requiresInstance\":false},{\"name\":\"L1_WATT\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.1.5\",\"requiresInstance\":false},{\"name\":\"L2_WATT\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.2.5\",\"requiresInstance\":false},{\"name\":\"L3_WATT\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.3.5\",\"requiresInstance\":false},{\"name\":\"L1_AMP\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.1.1\",\"requiresInstance\":false},{\"name\":\"L2_AMP\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.2.1\",\"requiresInstance\":false},{\"name\":\"L3_AMP\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.3.1\",\"requiresInstance\":false},{\"name\":\"L1_KWH\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.1.8\",\"requiresInstance\":false},{\"name\":\"L2_KWH\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.2.8\",\"requiresInstance\":false},{\"name\":\"L3_KWH\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.3.8\",\"requiresInstance\":false}],\"targets\":[{\"deviceId\":17,\"host\":\"14.42.43.207\",\"port\":30163,\"instanceId\":null},{\"deviceId\":18,\"host\":\"14.42.43.207\",\"port\":30165,\"instanceId\":null}],\"skipped\":[]}','2f379fa2-5ead-41c1-9d10-c1782409ef1e',1,'2026-08-20 04:20:27.751137','2026-08-26 06:49:48.704071'),
(4,2,'5분 그룹','0 */5 * * * *','{\"taskId\":2,\"groupId\":4,\"modelId\":10,\"protocol\":\"snmp\",\"cronExpression\":\"0 */5 * * * *\",\"community\":\"public\",\"timeoutMs\":2000,\"retries\":1,\"maxConcurrency\":10,\"oids\":[{\"name\":\"AMP\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.1\",\"requiresInstance\":false},{\"name\":\"PF\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.7\",\"requiresInstance\":false},{\"name\":\"TOTAL_WT\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.5\",\"requiresInstance\":false},{\"name\":\"TOTAL_KWH\",\"template\":\"1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.8\",\"requiresInstance\":false}],\"targets\":[{\"deviceId\":12,\"host\":\"14.42.43.207\",\"port\":30265,\"instanceId\":null},{\"deviceId\":13,\"host\":\"14.42.43.207\",\"port\":30266,\"instanceId\":null}],\"skipped\":[]}','18b3f50a-9d97-44c6-a162-df2ad56acfa5',1,'2026-08-20 04:57:22.315125','2026-08-25 07:47:33.789015');
/*!40000 ALTER TABLE `collection_task_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `common_code`
--

DROP TABLE IF EXISTS `common_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `common_code` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '공통 코드 ID',
  `group_id` int(11) NOT NULL COMMENT '코드 그룹 ID (FK)',
  `code` varchar(100) NOT NULL COMMENT '코드 값 (예: ups, pdu)',
  `name` varchar(255) NOT NULL COMMENT '코드 표시명',
  `sort_order` int(11) DEFAULT NULL COMMENT '정렬 순서',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_common_code_group_id_code` (`group_id`,`code`),
  KEY `idx_common_code_group_id` (`group_id`),
  CONSTRAINT `fk_common_code_group_id` FOREIGN KEY (`group_id`) REFERENCES `code_group` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공통 코드';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `common_code`
--

LOCK TABLES `common_code` WRITE;
/*!40000 ALTER TABLE `common_code` DISABLE KEYS */;
INSERT INTO `common_code` VALUES
(3,2,'pdu','PDU',0,'2026-07-01 06:12:43.711153','2026-07-01 06:12:43.711153'),
(4,2,'ups','UPS',0,'2026-07-01 06:12:54.001382','2026-07-01 06:13:51.690930'),
(5,3,'container','CONTAINER',0,'2026-07-01 06:14:04.194508','2026-07-01 06:14:04.194508'),
(6,3,'row','ROW',2,'2026-07-01 06:14:12.195697','2026-07-02 07:46:33.606795'),
(7,3,'rack','RACK',3,'2026-07-01 06:14:30.912303','2026-07-02 07:46:33.609682'),
(8,3,'zone','ZONE',1,'2026-07-02 07:46:24.845326','2026-07-02 07:46:33.612217'),
(9,4,'snmp','SNMP',1,'2026-07-06 00:36:05.187256','2026-07-06 00:36:05.187256'),
(10,4,'modbus','Modbus',2,'2026-07-06 00:36:05.187256','2026-07-06 00:36:05.187256'),
(11,4,'mqtt','MQTT',3,'2026-07-06 00:36:05.187256','2026-07-06 00:36:05.187256'),
(12,3,'UNASSIGNED','미배정',-1,'2026-07-22 00:26:09.797846','2026-07-22 00:26:09.797846'),
(13,2,'dragino','DRAGINO',NULL,'2026-07-22 06:15:03.933690','2026-07-22 06:15:08.238951'),
(14,2,'cdu','CDU',NULL,'2026-07-22 06:37:03.276710','2026-07-22 06:37:03.276710'),
(17,2,'rdc','RDC',NULL,'2026-07-29 02:15:57.044274','2026-07-29 04:21:27.045605'),
(18,2,'distribution-board','DISTRIBUTION BOARD',NULL,'2026-07-29 05:28:32.040974','2026-07-29 05:28:32.040974'),
(19,5,'environment','Environment',1,'2026-08-14 05:10:02.737208','2026-08-25 05:19:14.017526'),
(20,5,'cooling','Cooling',2,'2026-08-14 05:10:02.742498','2026-08-25 05:19:14.019583'),
(21,5,'analysis','Analysis',3,'2026-08-14 05:10:02.746661','2026-08-25 05:19:14.022007'),
(22,5,'power','Power',4,'2026-08-14 05:10:02.751748','2026-08-25 05:19:14.024146'),
(23,5,'dashboard','dashboard',NULL,'2026-08-25 05:19:14.013376','2026-08-25 05:19:14.013376'),
(24,2,'SENSOR','Sensor',3,'2026-08-26 04:09:28.164278','2026-08-26 04:09:28.164278'),
(25,2,'DISTRIBUTION_BOARD','Distribution Board',6,'2026-08-26 04:09:28.164278','2026-08-26 04:09:28.164278'),
(26,2,'OTHER','Other',99,'2026-08-26 04:09:28.164278','2026-08-26 04:09:28.164278'),
(27,6,'A','A Path',1,'2026-08-27 06:03:49.099390','2026-08-27 06:03:49.099390'),
(28,6,'B','B Path',2,'2026-08-27 06:03:49.099390','2026-08-27 06:03:49.099390'),
(29,6,'C','C Path',3,'2026-08-27 06:03:49.099390','2026-08-27 06:03:49.099390');
/*!40000 ALTER TABLE `common_code` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_model`
--

DROP TABLE IF EXISTS `device_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_model` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '장비 모델 ID',
  `name` varchar(255) NOT NULL COMMENT '모델/제품명',
  `manufacturer` varchar(255) NOT NULL COMMENT '제조사',
  `device_type_id` int(11) NOT NULL COMMENT 'common_code.id (모델 유형)',
  `description` varchar(1000) DEFAULT NULL COMMENT '설명',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_name_manufacturer` (`name`,`manufacturer`),
  KEY `idx_device_model_device_type_id` (`device_type_id`),
  CONSTRAINT `fk_device_model_device_type_id` FOREIGN KEY (`device_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 제품 모델 (SKU/제품군)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_model`
--

LOCK TABLES `device_model` WRITE;
/*!40000 ALTER TABLE `device_model` DISABLE KEYS */;
INSERT INTO `device_model` VALUES
(3,'PDU-3P','Raritan',3,'전력 측정','2026-07-07 08:01:49.092344','2026-08-19 07:24:27.652161'),
(9,'PDU-1P','OEM-6375',3,'단상 PDU (OID enterprise 6375)','2026-08-19 07:24:04.643390','2026-08-19 07:24:04.643390'),
(10,'PDU-1P','Raritan',3,'단상 PDU phase-1 (OID enterprise 13742)','2026-08-20 02:25:45.706545','2026-08-20 02:25:45.706545');
/*!40000 ALTER TABLE `device_model` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_model_modbus_point`
--

DROP TABLE IF EXISTS `device_model_modbus_point`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_model_modbus_point` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Modbus point ID',
  `model_protocol_id` int(11) NOT NULL COMMENT 'device_model_protocol.id (FK)',
  `name` varchar(255) NOT NULL COMMENT '식별자·표시명 (TOTAL_WT, ONTO-TEMP 등)',
  `register_type` varchar(30) NOT NULL COMMENT '레지스터 종류 (COIL/DISCRETE/HOLDING/INPUT)',
  `data_type` varchar(20) NOT NULL COMMENT '값 해석 타입 (INT16/UINT16/INT32/UINT32/FLOAT32)',
  `byte_order` varchar(10) DEFAULT NULL COMMENT '멀티 레지스터 바이트 순서 (ABCD/CDAB/BADC/DCBA), 단일이면 NULL',
  `address` int(11) DEFAULT NULL COMMENT '레지스터 주소 (0~65535). requires_instance=1이면 NULL',
  `requires_instance` tinyint(1) NOT NULL DEFAULT 0 COMMENT '주소를 인스턴스가 제공하는지 (0=false, 1=true)',
  `scale` double DEFAULT NULL COMMENT '원시값에 곱할 배율 (NULL이면 1)',
  `unit` varchar(50) DEFAULT NULL COMMENT '단위 (W, A, °C, % 등)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_modbus_point_protocol_name` (`model_protocol_id`,`name`),
  KEY `idx_device_model_modbus_point_model_protocol_id` (`model_protocol_id`),
  CONSTRAINT `fk_device_model_modbus_point_model_protocol_id` FOREIGN KEY (`model_protocol_id`) REFERENCES `device_model_protocol` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_device_model_modbus_point_requires_instance` CHECK (`requires_instance` in (0,1)),
  CONSTRAINT `chk_device_model_modbus_point_enabled` CHECK (`enabled` in (0,1)),
  CONSTRAINT `chk_device_model_modbus_point_address` CHECK (`requires_instance` = 1 and `address` is null or `requires_instance` = 0 and `address` is not null and `address` between 0 and 65535)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 모델별 Modbus 수집 point (레지스터 카탈로그)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_model_modbus_point`
--

LOCK TABLES `device_model_modbus_point` WRITE;
/*!40000 ALTER TABLE `device_model_modbus_point` DISABLE KEYS */;
/*!40000 ALTER TABLE `device_model_modbus_point` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_model_protocol`
--

DROP TABLE IF EXISTS `device_model_protocol`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_model_protocol` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '모델-프로토콜 연결 ID',
  `model_id` int(11) NOT NULL COMMENT 'device_model.id (FK)',
  `protocol_type_id` int(11) NOT NULL COMMENT 'common_code.id (PROTOCOL_TYPE만)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_protocol_model_protocol` (`model_id`,`protocol_type_id`),
  KEY `idx_device_model_protocol_model_id` (`model_id`),
  KEY `idx_device_model_protocol_protocol_type_id` (`protocol_type_id`),
  CONSTRAINT `fk_device_model_protocol_model_id` FOREIGN KEY (`model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_device_model_protocol_protocol_type_id` FOREIGN KEY (`protocol_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 모델별 지원 프로토콜 (N:M 연결)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_model_protocol`
--

LOCK TABLES `device_model_protocol` WRITE;
/*!40000 ALTER TABLE `device_model_protocol` DISABLE KEYS */;
INSERT INTO `device_model_protocol` VALUES
(16,3,9,'2026-07-23 06:06:09.895828','2026-07-23 06:06:09.895828'),
(23,9,9,'2026-08-19 07:24:04.646984','2026-08-19 07:24:04.646984'),
(24,10,9,'2026-08-20 02:25:45.710803','2026-08-20 02:25:45.710803');
/*!40000 ALTER TABLE `device_model_protocol` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_model_snmp_point`
--

DROP TABLE IF EXISTS `device_model_snmp_point`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_model_snmp_point` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'SNMP point ID',
  `model_protocol_id` int(11) NOT NULL COMMENT 'device_model_protocol.id (FK)',
  `name` varchar(255) NOT NULL COMMENT '식별자·표시명 (V, 전압, PRI-FLOW 등)',
  `oid` varchar(512) NOT NULL COMMENT 'SNMP OID 또는 {instanceId} 템플릿',
  `requires_instance` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'instanceId 치환 필요 여부 (0=false, 1=true)',
  `unit` varchar(50) DEFAULT NULL COMMENT '단위 (V, A, L/min 등)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_snmp_point_protocol_name` (`model_protocol_id`,`name`),
  UNIQUE KEY `uk_device_model_snmp_point_protocol_oid` (`model_protocol_id`,`oid`),
  KEY `idx_device_model_snmp_point_model_protocol_id` (`model_protocol_id`),
  CONSTRAINT `fk_device_model_snmp_point_model_protocol_id` FOREIGN KEY (`model_protocol_id`) REFERENCES `device_model_protocol` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 모델별 SNMP 수집 point (OID 카탈로그)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_model_snmp_point`
--

LOCK TABLES `device_model_snmp_point` WRITE;
/*!40000 ALTER TABLE `device_model_snmp_point` DISABLE KEYS */;
INSERT INTO `device_model_snmp_point` VALUES
(9,23,'V','1.3.6.1.4.1.6375.1.1.0',0,'V',1,'2026-08-19 07:24:04.657814','2026-08-19 07:24:04.657814'),
(10,23,'status','1.3.6.1.4.1.6375.1.2.0',0,NULL,1,'2026-08-19 07:24:04.657814','2026-08-19 07:24:04.657814'),
(11,23,'Hz','1.3.6.1.4.1.6375.1.3.0',0,'Hz',1,'2026-08-19 07:24:04.657814','2026-08-19 07:24:04.657814'),
(12,23,'flag','1.3.6.1.4.1.6375.1.4.0',0,NULL,1,'2026-08-19 07:24:04.657814','2026-08-19 07:24:04.657814'),
(13,23,'reserved5','1.3.6.1.4.1.6375.1.5.0',0,NULL,1,'2026-08-19 07:24:04.657814','2026-08-19 07:24:04.657814'),
(14,23,'reserved6','1.3.6.1.4.1.6375.1.6.0',0,NULL,1,'2026-08-19 07:24:04.657814','2026-08-19 07:24:04.657814'),
(15,23,'reserved7','1.3.6.1.4.1.6375.1.7.0',0,NULL,1,'2026-08-19 07:24:04.657814','2026-08-19 07:24:04.657814'),
(16,23,'W','1.3.6.1.4.1.6375.1.8.0',0,'W',1,'2026-08-19 07:24:04.657814','2026-08-19 07:24:04.657814'),
(17,23,'reserved9','1.3.6.1.4.1.6375.1.9.0',0,NULL,1,'2026-08-19 07:24:04.657814','2026-08-19 07:24:04.657814'),
(24,24,'AMP','1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.1',0,'A',1,'2026-08-20 02:25:45.716771','2026-08-20 02:25:45.716771'),
(25,24,'PF','1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.7',0,NULL,1,'2026-08-20 02:25:45.716771','2026-08-20 02:25:45.716771'),
(26,24,'TOTAL_WT','1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.5',0,'W',1,'2026-08-20 02:25:45.716771','2026-08-20 02:25:45.716771'),
(27,24,'TOTAL_KWH','1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.8',0,'kWh',1,'2026-08-20 02:25:45.716771','2026-08-20 02:25:45.716771'),
(31,16,'TOTAL_WT','1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.5',0,'W',1,'2026-08-20 02:50:32.067219','2026-08-20 02:50:32.067219'),
(32,16,'AMP','1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.1',0,'A',1,'2026-08-20 02:50:52.096844','2026-08-20 02:50:52.096844'),
(33,16,'PF','1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.7',0,NULL,1,'2026-08-20 02:50:55.073322','2026-08-20 02:50:55.073322'),
(34,16,'TOTAL_KWH','1.3.6.1.4.1.13742.6.5.2.3.1.4.1.1.8',0,'kWh',1,'2026-08-20 02:50:58.198873','2026-08-20 02:50:58.198873'),
(35,16,'L1_WATT','1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.1.5',0,'W',1,'2026-08-20 02:51:02.176585','2026-08-20 02:51:02.176585'),
(36,16,'L2_WATT','1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.2.5',0,'W',1,'2026-08-20 02:51:04.801817','2026-08-20 02:51:04.801817'),
(37,16,'L3_WATT','1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.3.5',0,'W',1,'2026-08-20 02:51:07.091239','2026-08-20 02:51:07.091239'),
(38,16,'L1_AMP','1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.1.1',0,'A',1,'2026-08-20 02:51:09.775460','2026-08-20 02:51:09.775460'),
(39,16,'L2_AMP','1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.2.1',0,'A',1,'2026-08-20 02:51:12.459723','2026-08-20 02:51:12.459723'),
(40,16,'L3_AMP','1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.3.1',0,'A',1,'2026-08-20 02:51:14.607688','2026-08-20 02:51:14.607688'),
(41,16,'L1_KWH','1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.1.8',0,'kWh',1,'2026-08-20 02:51:16.818033','2026-08-20 02:51:16.818033'),
(42,16,'L2_KWH','1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.2.8',0,'kWh',1,'2026-08-20 02:51:19.546440','2026-08-20 02:51:19.546440'),
(43,16,'L3_KWH','1.3.6.1.4.1.13742.6.5.2.4.1.4.1.1.3.8',0,'kWh',1,'2026-08-20 02:51:22.513330','2026-08-20 02:51:22.513330');
/*!40000 ALTER TABLE `device_model_snmp_point` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_protocol_endpoint`
--

DROP TABLE IF EXISTS `device_protocol_endpoint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_protocol_endpoint` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '엔드포인트 ID',
  `device_id` int(11) NOT NULL COMMENT 'devices.id (FK)',
  `protocol_type_id` int(11) NOT NULL COMMENT 'common_code.id (PROTOCOL_TYPE만)',
  `host` varchar(255) NOT NULL COMMENT 'IP 또는 hostname',
  `port` int(11) NOT NULL COMMENT '포트 (1~65535)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_protocol_endpoint_device_protocol` (`device_id`,`protocol_type_id`),
  UNIQUE KEY `uk_device_protocol_endpoint_host_port` (`host`,`port`),
  KEY `idx_device_protocol_endpoint_device_id` (`device_id`),
  KEY `idx_device_protocol_endpoint_protocol_type_id` (`protocol_type_id`),
  CONSTRAINT `fk_device_protocol_endpoint_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_protocol_endpoint_protocol_type_id` FOREIGN KEY (`protocol_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `chk_device_protocol_endpoint_enabled` CHECK (`enabled` in (0,1)),
  CONSTRAINT `chk_device_protocol_endpoint_port` CHECK (`port` >= 1 and `port` <= 65535)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 프로토콜 엔드포인트 (host/port 공통 전송층)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_protocol_endpoint`
--

LOCK TABLES `device_protocol_endpoint` WRITE;
/*!40000 ALTER TABLE `device_protocol_endpoint` DISABLE KEYS */;
INSERT INTO `device_protocol_endpoint` VALUES
(3,9,9,'192.168.14.114',161,1,'2026-08-19 07:24:04.684251','2026-08-19 07:24:04.684251'),
(4,10,9,'14.42.43.207',30263,1,'2026-08-20 02:25:45.738057','2026-08-20 02:25:45.738057'),
(5,11,9,'14.42.43.207',30264,1,'2026-08-20 02:25:45.738057','2026-08-26 04:27:57.581300'),
(6,12,9,'14.42.43.207',30265,1,'2026-08-20 02:25:45.738057','2026-08-20 02:25:45.738057'),
(7,13,9,'14.42.43.207',30266,1,'2026-08-20 02:25:45.738057','2026-08-20 02:25:45.738057'),
(11,17,9,'14.42.43.207',30163,1,'2026-08-20 04:17:17.826450','2026-08-20 05:12:48.138405'),
(12,18,9,'14.42.43.207',30165,1,'2026-08-26 06:49:48.655934','2026-08-26 06:49:48.655934');
/*!40000 ALTER TABLE `device_protocol_endpoint` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_snmp_instance`
--

DROP TABLE IF EXISTS `device_snmp_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_snmp_instance` (
  `endpoint_id` int(11) NOT NULL COMMENT 'device_protocol_endpoint.id (PK/FK, SNMP endpoint 1:1)',
  `instance_id` int(11) NOT NULL COMMENT 'SNMP MIB instance index ({instanceId} 치환값)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`endpoint_id`),
  CONSTRAINT `fk_device_snmp_instance_endpoint_id` FOREIGN KEY (`endpoint_id`) REFERENCES `device_protocol_endpoint` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_device_snmp_instance_id` CHECK (`instance_id` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 SNMP instance 인덱스 (OID {instanceId} 치환, 필요한 endpoint만)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_snmp_instance`
--

LOCK TABLES `device_snmp_instance` WRITE;
/*!40000 ALTER TABLE `device_snmp_instance` DISABLE KEYS */;
/*!40000 ALTER TABLE `device_snmp_instance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `devices`
--

DROP TABLE IF EXISTS `devices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `devices` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '장비 ID (API {deviceId}, Influx tag device_id)',
  `model_id` int(11) NOT NULL COMMENT 'device_model.id (FK) — 제품 카탈로그',
  `location_node_code` char(10) NOT NULL COMMENT 'location_node.code (FK, 필수 — 미지정 시 UNASSIGNED)',
  `name` varchar(255) NOT NULL COMMENT '현장 표시명',
  `description` varchar(1000) DEFAULT NULL COMMENT '설명',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_devices_location_node_code_name` (`location_node_code`,`name`),
  KEY `idx_devices_model_enabled` (`model_id`,`enabled`),
  KEY `idx_devices_location_enabled` (`location_node_code`,`enabled`),
  CONSTRAINT `fk_devices_location_node_code` FOREIGN KEY (`location_node_code`) REFERENCES `location_node` (`code`) ON UPDATE CASCADE,
  CONSTRAINT `fk_devices_model_id` FOREIGN KEY (`model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `chk_devices_enabled` CHECK (`enabled` in (0,1))
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 인스턴스 (현장 1대 = 1행, 얇은 인스턴스층)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `devices`
--

LOCK TABLES `devices` WRITE;
/*!40000 ALTER TABLE `devices` DISABLE KEYS */;
INSERT INTO `devices` VALUES
(9,9,'PD1RACK114','VIVANS-PDU','192.168.14.114 단상 PDU',1,'2026-08-19 07:24:04.668494','2026-08-26 06:46:45.894365'),
(10,10,'PD1RACK207','WallCoil-RACK22A-PDU','14.42.43.207:30263 phase-1 Raritan PDU',1,'2026-08-20 02:25:45.723818','2026-08-26 06:47:25.883266'),
(11,10,'PD1RACK207','WallCoil-RACK22B-PDU','14.42.43.207:30264 phase-1 Raritan PDU',1,'2026-08-20 02:25:45.723818','2026-08-26 06:47:38.073264'),
(12,10,'PD1RACK207','WallCoil-RACK23A-PDU','14.42.43.207:30265 phase-1 Raritan PDU',1,'2026-08-20 02:25:45.723818','2026-08-26 06:47:52.431325'),
(13,10,'PD1RACK207','WallCoil-RACK23B-PDU','14.42.43.207:30266 phase-1 Raritan PDU',1,'2026-08-20 02:25:45.723818','2026-08-26 06:48:04.095556'),
(17,3,'F2MEQMx2TV','In-Row-Cooler-RACK42B-PDU','14.42.43.207:30164 phase-3 Raritan PDU',1,'2026-08-20 04:16:12.744481','2026-08-26 06:48:37.079186'),
(18,3,'JgasKlAgFy','In-Row-Cooler-RACK43B-PDU',NULL,1,'2026-08-26 06:49:48.540822','2026-08-26 06:49:48.540822');
/*!40000 ALTER TABLE `devices` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `location_node`
--

DROP TABLE IF EXISTS `location_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `location_node` (
  `code` char(10) NOT NULL COMMENT '노드 PK (일반: 10자 Base62 / 시스템: UNASSIGNED)',
  `parent_code` char(10) DEFAULT NULL COMMENT '부모 노드 code (루트는 NULL)',
  `location_type_id` int(11) NOT NULL COMMENT '위치 유형 ID (FK → common_code, LOCATION_TYPE만)',
  `path_code_id` int(11) DEFAULT NULL COMMENT 'LOCATION_PATH common_code.id (차트 Path 그룹)',
  `name` varchar(255) NOT NULL COMMENT '노드 표시명',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`code`),
  UNIQUE KEY `uk_location_node_parent_code_name` (`parent_code`,`name`),
  KEY `idx_location_node_parent_code` (`parent_code`),
  KEY `idx_location_node_location_type_id` (`location_type_id`),
  KEY `idx_location_node_path_code_id` (`path_code_id`),
  CONSTRAINT `fk_location_node_location_type_id` FOREIGN KEY (`location_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_location_node_parent_code` FOREIGN KEY (`parent_code`) REFERENCES `location_node` (`code`) ON UPDATE CASCADE,
  CONSTRAINT `fk_location_node_path_code_id` FOREIGN KEY (`path_code_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='위치 트리 노드';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `location_node`
--

LOCK TABLES `location_node` WRITE;
/*!40000 ALTER TABLE `location_node` DISABLE KEYS */;
INSERT INTO `location_node` VALUES
('F2MEQMx2TV','JgasKlAgFy',7,NULL,'Raritan 3상 PDU Rack','2026-08-20 04:13:23.830351','2026-08-20 04:13:23.830351'),
('GAls73Gg8h',NULL,5,NULL,'VIVANS 컨테이너','2026-08-26 06:24:27.857672','2026-08-26 06:24:27.857672'),
('JgasKlAgFy',NULL,8,NULL,'Raritan 3상 PDU Zone','2026-08-20 04:13:23.809797','2026-08-20 04:13:23.809797'),
('PD1RACK114','PD1ZONE114',7,NULL,'단상 PDU Rack','2026-08-19 07:24:04.638716','2026-08-19 07:24:04.638716'),
('PD1RACK207','PD1ZONE207',7,NULL,'Raritan PDU Rack','2026-08-20 02:25:45.699781','2026-08-20 02:25:45.699781'),
('PD1ZONE114',NULL,8,NULL,'단상 PDU Zone','2026-08-19 07:24:04.633705','2026-08-19 07:24:04.633705'),
('PD1ZONE207',NULL,8,NULL,'Raritan PDU Zone','2026-08-20 02:25:45.694350','2026-08-20 02:25:45.694350'),
('UNASSIGNED',NULL,12,NULL,'미배정','2026-07-22 00:26:09.802320','2026-07-22 00:26:09.802320');
/*!40000 ALTER TABLE `location_node` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `page_widget`
--

DROP TABLE IF EXISTS `page_widget`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `page_widget` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '위젯 ID',
  `page_code_id` int(11) NOT NULL COMMENT 'common_code.id (DEVICE_PAGE)',
  `name` varchar(100) NOT NULL COMMENT '위젯 표시명',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
  `query_kind` varchar(16) NOT NULL COMMENT 'last | aggregate | count',
  `group_by` varchar(16) DEFAULT NULL COMMENT 'device | point | location',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_page_name` (`page_code_id`,`name`),
  KEY `idx_page_widget_page_code_id` (`page_code_id`),
  CONSTRAINT `fk_page_widget_page_code_id` FOREIGN KEY (`page_code_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `chk_page_widget_enabled` CHECK (`enabled` in (0,1)),
  CONSTRAINT `chk_page_widget_group_by` CHECK (`group_by` is null or `group_by` in ('device','point','location')),
  CONSTRAINT `chk_page_widget_query_kind` CHECK (`query_kind` in ('last','aggregate','count','chart'))
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='페이지 위젯 카드 정의 (DEVICE_PAGE 자식)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `page_widget`
--

LOCK TABLES `page_widget` WRITE;
/*!40000 ALTER TABLE `page_widget` DISABLE KEYS */;
INSERT INTO `page_widget` VALUES
(1,23,'PDU 전체 전력',1,'last',NULL,'2026-08-25 07:09:47.561829','2026-08-26 07:20:06.219915'),
(2,23,'PDU 전력 구성',1,'last','location','2026-08-25 07:09:47.568730','2026-08-26 05:56:29.669600'),
(5,23,'PDU 실시간',1,'last',NULL,'2026-08-25 07:09:47.611237','2026-08-26 05:56:42.834351'),
(6,23,'PDU 랙 전력 순위',0,'last','device','2026-08-25 07:09:47.684993','2026-08-26 05:04:45.443259'),
(9,23,'PDU 오늘 kWh',0,'aggregate',NULL,'2026-08-25 07:09:47.705562','2026-08-26 05:04:45.817102'),
(10,23,'PDU 당월 kWh',0,'aggregate',NULL,'2026-08-25 07:09:47.709859','2026-08-26 05:04:46.258258'),
(11,23,'PDU 전월 kWh',0,'aggregate',NULL,'2026-08-25 07:09:47.719231','2026-08-26 05:04:46.629146'),
(12,23,'PDU PF',0,'aggregate',NULL,'2026-08-25 07:09:47.723360','2026-08-26 05:04:47.019341'),
(13,23,'PUE',0,'aggregate',NULL,'2026-08-25 07:09:47.729527','2026-08-26 05:04:47.416848'),
(14,23,'PDU 에너지 비율',0,'aggregate','location','2026-08-25 07:09:47.733377','2026-08-26 05:04:48.310631'),
(15,23,'PDU 수',1,'count',NULL,'2026-08-25 07:09:47.737573','2026-08-27 00:09:27.756982'),
(16,23,'PDU-TEST',1,'last',NULL,'2026-08-26 07:19:46.186260','2026-08-26 07:20:09.285162'),
(19,22,'전력 차트',1,'chart',NULL,'2026-08-27 05:37:16.962046','2026-08-27 05:58:33.209245');
/*!40000 ALTER TABLE `page_widget` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `page_widget_aggregate`
--

DROP TABLE IF EXISTS `page_widget_aggregate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `page_widget_aggregate` (
  `widget_id` int(11) NOT NULL,
  `op` varchar(16) NOT NULL,
  `weight_point` varchar(100) DEFAULT NULL,
  `numerator_point` varchar(100) DEFAULT NULL,
  `denominator_point` varchar(100) DEFAULT NULL,
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`widget_id`),
  CONSTRAINT `fk_page_widget_aggregate_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_page_widget_aggregate_op` CHECK (`op` in ('delta_sum','weighted_avg','divide'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `page_widget_aggregate`
--

LOCK TABLES `page_widget_aggregate` WRITE;
/*!40000 ALTER TABLE `page_widget_aggregate` DISABLE KEYS */;
INSERT INTO `page_widget_aggregate` VALUES
(9,'delta_sum',NULL,NULL,NULL,'2026-08-27 04:16:21.849586','2026-08-27 04:16:21.849586'),
(10,'delta_sum',NULL,NULL,NULL,'2026-08-27 04:16:21.850551','2026-08-27 04:16:21.850551'),
(11,'delta_sum',NULL,NULL,NULL,'2026-08-27 04:16:21.851367','2026-08-27 04:16:21.851367'),
(12,'weighted_avg','W',NULL,NULL,'2026-08-27 04:16:21.851920','2026-08-27 04:16:21.851920'),
(13,'divide',NULL,'W','IT_POWER','2026-08-27 04:16:21.852245','2026-08-27 04:16:21.852245'),
(14,'delta_sum',NULL,NULL,NULL,'2026-08-27 04:16:21.852633','2026-08-27 04:16:21.852633');
/*!40000 ALTER TABLE `page_widget_aggregate` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `page_widget_chart`
--

DROP TABLE IF EXISTS `page_widget_chart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `page_widget_chart` (
  `widget_id` int(11) NOT NULL,
  `chart_scope` varchar(16) NOT NULL,
  `chart_series_mode` varchar(16) NOT NULL,
  `chart_range_preset` varchar(16) NOT NULL,
  `chart_window` varchar(8) NOT NULL,
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`widget_id`),
  CONSTRAINT `fk_pwch_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_pwch_scope` CHECK (`chart_scope` in ('devices','models')),
  CONSTRAINT `chk_pwch_series_mode` CHECK (`chart_series_mode` in ('per_device','sum','by_phase','by_path')),
  CONSTRAINT `chk_pwch_range_preset` CHECK (`chart_range_preset` in ('last_24h','today','yesterday','last_7d','this_month'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `page_widget_chart`
--

LOCK TABLES `page_widget_chart` WRITE;
/*!40000 ALTER TABLE `page_widget_chart` DISABLE KEYS */;
INSERT INTO `page_widget_chart` VALUES
(19,'devices','sum','last_24h','15m','2026-08-27 05:37:16.993149','2026-08-27 05:37:16.993149');
/*!40000 ALTER TABLE `page_widget_chart` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `page_widget_count`
--

DROP TABLE IF EXISTS `page_widget_count`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `page_widget_count` (
  `widget_id` int(11) NOT NULL,
  `count_mode` varchar(16) NOT NULL,
  `count_model_id` int(11) DEFAULT NULL,
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`widget_id`),
  KEY `idx_pwc_count_model_id` (`count_model_id`),
  CONSTRAINT `fk_pwc_model_id` FOREIGN KEY (`count_model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_pwc_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_pwc_count_mode` CHECK (`count_mode` in ('total','by_model','model'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `page_widget_count`
--

LOCK TABLES `page_widget_count` WRITE;
/*!40000 ALTER TABLE `page_widget_count` DISABLE KEYS */;
INSERT INTO `page_widget_count` VALUES
(15,'by_model',NULL,'2026-08-27 04:16:21.853308','2026-08-27 04:16:21.853308');
/*!40000 ALTER TABLE `page_widget_count` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `page_widget_device`
--

DROP TABLE IF EXISTS `page_widget_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `page_widget_device` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `device_id` int(11) NOT NULL COMMENT 'devices.id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_device_widget_device` (`widget_id`,`device_id`),
  KEY `idx_page_widget_device_widget_id` (`widget_id`),
  KEY `idx_page_widget_device_device_id` (`device_id`),
  CONSTRAINT `fk_page_widget_device_device_id` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_page_widget_device_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=138 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='페이지 위젯 조회 장비 (1:N)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `page_widget_device`
--

LOCK TABLES `page_widget_device` WRITE;
/*!40000 ALTER TABLE `page_widget_device` DISABLE KEYS */;
INSERT INTO `page_widget_device` VALUES
(44,1,9),
(55,1,10),
(66,1,11),
(77,1,12),
(88,1,13),
(22,1,17),
(42,2,9),
(53,2,10),
(64,2,11),
(75,2,12),
(86,2,13),
(20,2,17),
(35,5,9),
(46,5,10),
(57,5,11),
(68,5,12),
(79,5,13),
(13,5,17),
(39,6,9),
(50,6,10),
(61,6,11),
(72,6,12),
(83,6,13),
(17,6,17),
(41,9,9),
(52,9,10),
(63,9,11),
(74,9,12),
(85,9,13),
(19,9,17),
(38,10,9),
(49,10,10),
(60,10,11),
(71,10,12),
(82,10,13),
(16,10,17),
(43,11,9),
(54,11,10),
(65,11,11),
(76,11,12),
(87,11,13),
(21,11,17),
(36,12,9),
(47,12,10),
(58,12,11),
(69,12,12),
(80,12,13),
(14,12,17),
(37,13,9),
(48,13,10),
(59,13,11),
(70,13,12),
(81,13,13),
(15,13,17),
(40,14,9),
(51,14,10),
(62,14,11),
(73,14,12),
(84,14,13),
(18,14,17),
(34,15,9),
(45,15,10),
(56,15,11),
(67,15,12),
(78,15,13),
(12,15,17),
(128,16,18),
(136,19,17),
(137,19,18);
/*!40000 ALTER TABLE `page_widget_device` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `page_widget_layout`
--

DROP TABLE IF EXISTS `page_widget_layout`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `page_widget_layout` (
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id (PK, 1:1)',
  `grid_x` int(11) NOT NULL COMMENT '그리드 X',
  `grid_y` int(11) NOT NULL COMMENT '그리드 Y',
  `w` int(11) NOT NULL COMMENT '가로 칸 수',
  `h` int(11) NOT NULL COMMENT '세로 칸 수',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`widget_id`),
  CONSTRAINT `fk_page_widget_layout_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_page_widget_layout_grid_x` CHECK (`grid_x` >= 0),
  CONSTRAINT `chk_page_widget_layout_grid_y` CHECK (`grid_y` >= 0),
  CONSTRAINT `chk_page_widget_layout_w` CHECK (`w` >= 1),
  CONSTRAINT `chk_page_widget_layout_h` CHECK (`h` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='페이지 위젯 UI 그리드 배치 2D (1:1)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `page_widget_layout`
--

LOCK TABLES `page_widget_layout` WRITE;
/*!40000 ALTER TABLE `page_widget_layout` DISABLE KEYS */;
INSERT INTO `page_widget_layout` VALUES
(1,16,0,14,7,'2026-08-26 05:03:30.315161','2026-08-26 08:04:20.213866'),
(2,18,8,20,8,'2026-08-26 05:03:24.104340','2026-08-26 08:05:50.910874'),
(5,0,8,16,8,'2026-08-26 05:03:31.688229','2026-08-26 08:05:49.743735'),
(6,0,2,4,2,'2026-08-26 05:03:45.035293','2026-08-26 05:03:45.035293'),
(9,7,2,4,2,'2026-08-26 05:03:40.411352','2026-08-26 05:04:03.258060'),
(10,3,2,4,2,'2026-08-26 05:03:41.957970','2026-08-26 05:04:04.723485'),
(11,0,4,4,2,'2026-08-26 05:03:38.765055','2026-08-26 05:03:38.765055'),
(12,8,4,4,2,'2026-08-26 05:03:34.709903','2026-08-26 05:03:34.709903'),
(13,4,4,4,2,'2026-08-26 05:03:35.967484','2026-08-26 05:03:35.967484'),
(14,8,0,4,2,'2026-08-26 05:03:47.132458','2026-08-26 05:04:26.699994'),
(15,0,0,16,7,'2026-08-26 05:03:56.623409','2026-08-26 08:03:40.748875'),
(16,30,0,16,7,'2026-08-26 07:19:46.215935','2026-08-26 08:04:25.587792'),
(19,0,0,26,11,'2026-08-27 05:37:17.006758','2026-08-27 05:58:37.866785');
/*!40000 ALTER TABLE `page_widget_layout` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `page_widget_model`
--

DROP TABLE IF EXISTS `page_widget_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `page_widget_model` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `model_id` int(11) NOT NULL COMMENT 'device_model.id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_model_widget_model` (`widget_id`,`model_id`),
  KEY `idx_page_widget_model_widget_id` (`widget_id`),
  KEY `idx_page_widget_model_model_id` (`model_id`),
  CONSTRAINT `fk_page_widget_model_model_id` FOREIGN KEY (`model_id`) REFERENCES `device_model` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_page_widget_model_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='chart widget model scope (1:N)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `page_widget_model`
--

LOCK TABLES `page_widget_model` WRITE;
/*!40000 ALTER TABLE `page_widget_model` DISABLE KEYS */;
/*!40000 ALTER TABLE `page_widget_model` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `page_widget_point`
--

DROP TABLE IF EXISTS `page_widget_point`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `page_widget_point` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `point_name` varchar(100) NOT NULL COMMENT 'Influx point_name',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_point_widget_name` (`widget_id`,`point_name`),
  KEY `idx_page_widget_point_widget_id` (`widget_id`),
  CONSTRAINT `fk_page_widget_point_widget_id` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='페이지 위젯 조회 포인트 (1:N)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `page_widget_point`
--

LOCK TABLES `page_widget_point` WRITE;
/*!40000 ALTER TABLE `page_widget_point` DISABLE KEYS */;
INSERT INTO `page_widget_point` VALUES
(34,1,'TOTAL_WT'),
(1,1,'W'),
(36,2,'TOTAL_WT'),
(2,2,'W'),
(11,5,'AMP'),
(13,5,'L1_WATT'),
(14,5,'L2_WATT'),
(15,5,'L3_WATT'),
(10,5,'PF'),
(39,5,'TOTAL_WT'),
(12,5,'V'),
(9,5,'W'),
(38,6,'TOTAL_WT'),
(16,6,'W'),
(20,9,'TOTAL_KWH'),
(21,10,'TOTAL_KWH'),
(22,11,'TOTAL_KWH'),
(23,12,'PF'),
(24,14,'TOTAL_KWH'),
(40,16,'TOTAL_WT'),
(49,19,'L1_WATT'),
(50,19,'L2_WATT'),
(51,19,'L3_WATT'),
(52,19,'TOTAL_WT');
/*!40000 ALTER TABLE `page_widget_point` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
  `username` varchar(255) NOT NULL COMMENT '로그인 아이디',
  `password` varchar(255) NOT NULL COMMENT 'BCrypt 해시 비밀번호',
  `role` varchar(50) DEFAULT NULL COMMENT '권한 (기본 USER)',
  `refresh_token` varchar(512) DEFAULT NULL COMMENT 'JWT refresh token',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES
(1,'test','$2a$10$/4t9VgffCTWugHn0Ts3gxugF2Ol4XSFS0jsLAwGN9v0sZJqeNMC6G','USER','eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzg3ODA3NDU3LCJleHAiOjE4NjY2NDc0NTd9.cIEi04XepoQko4ntOl1YrP269_oN29DYsiSK5EEnwgA','2026-06-26 05:36:31.467973','2026-08-27 05:10:57.503540'),
(2,'opsuser4826','$2a$10$Yc4KXdoVMSEiuMrHtDem0.zQLIdgizDDPrCxXuDlP9y4u1gBJ1o4.','USER','eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJvcHN1c2VyNDgyNiIsImlhdCI6MTc4NzcyODM3NywiZXhwIjoxODY2NTY4Mzc3fQ.hc6hF5pPg5w_iJZy0VlwVX8qmPiE48biTGUWBWD3lfI','2026-08-26 07:12:57.215337','2026-08-26 07:12:57.406155');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'dcim_new'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-27 15:17:48
