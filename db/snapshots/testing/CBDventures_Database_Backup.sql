CREATE DATABASE  IF NOT EXISTS `cbdventures_db_clean` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `cbdventures_db_clean`;
-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: cbdventures_db_clean
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `amenity`
--

DROP TABLE IF EXISTS `amenity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `amenity` (
  `amenity_id` int unsigned NOT NULL AUTO_INCREMENT,
  `amenity_name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`amenity_id`),
  UNIQUE KEY `amenity_name` (`amenity_name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `amenity`
--

LOCK TABLES `amenity` WRITE;
/*!40000 ALTER TABLE `amenity` DISABLE KEYS */;
INSERT INTO `amenity` VALUES (1,'Free Wi-Fi','Wireless internet access for customers',1),(2,'Power Outlets','Electrical power outlets available for customers',1),(3,'Quiet Environment','Suitable for studying or quiet work',1),(4,'Student Discount','Discount or special pricing for students',1),(5,'Backup Power','Alternative power available during electricity interruptions',1);
/*!40000 ALTER TABLE `amenity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `business`
--

DROP TABLE IF EXISTS `business`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `business` (
  `business_id` int unsigned NOT NULL AUTO_INCREMENT,
  `owner_id` int unsigned DEFAULT NULL,
  `category_id` int unsigned NOT NULL,
  `name` varchar(150) NOT NULL,
  `description` text,
  `address` varchar(255) NOT NULL,
  `latitude` decimal(10,7) NOT NULL,
  `longitude` decimal(10,7) NOT NULL,
  `contact_phone` varchar(30) DEFAULT NULL,
  `contact_email` varchar(150) DEFAULT NULL,
  `price_level` tinyint unsigned DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED','INACTIVE') NOT NULL DEFAULT 'PENDING',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`business_id`),
  KEY `idx_business_owner` (`owner_id`),
  KEY `idx_business_category` (`category_id`),
  KEY `idx_business_status` (`status`),
  KEY `idx_business_location` (`latitude`,`longitude`),
  CONSTRAINT `fk_business_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`),
  CONSTRAINT `fk_business_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `chk_business_price` CHECK (((`price_level` is null) or (`price_level` between 1 and 3)))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `business`
--

LOCK TABLES `business` WRITE;
/*!40000 ALTER TABLE `business` DISABLE KEYS */;
INSERT INTO `business` VALUES (1,NULL,3,'SA Queen Hair & Beauty','Hair and beauty salon offering hair and nail services.','231 Church St, Pretoria Central, Pretoria, 0002, South Africa',-25.7465700,28.1892000,'072 676 2797',NULL,NULL,'APPROVED','2026-10-02 19:15:56','2026-10-13 21:20:19'),(2,NULL,2,'Black Bistro Lounge & Cocktail Kitchen','Bistro and cocktail kitchen serving meals and drinks at the South African State Theatre.','Drama Theatre Entrance, 320 Pretorius St, Pretoria Central, 0002, South Africa',-25.7467600,28.1947200,'+27 12 320 0206',NULL,NULL,'APPROVED','2026-10-02 19:15:56','2026-10-13 21:50:47'),(3,NULL,4,'Es\'kia Mphahlele Community Library','Public library providing reading, research and information services.','347 Madiba St, Pretoria Central, Pretoria, 0002, South Africa',-25.7446296,28.1948915,'+27 12 358 8957',NULL,NULL,'APPROVED','2026-10-02 19:15:56','2026-10-13 21:27:09');
/*!40000 ALTER TABLE `business` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_business_within_1km_insert` BEFORE INSERT ON `business` FOR EACH ROW BEGIN
    IF ST_Distance_Sphere(
        POINT(NEW.longitude, NEW.latitude),
        POINT(28.192483, -25.746400)
    ) > 1000 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Business location must be within 1 km of Richfield Pretoria Campus.';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_business_within_1km_update` BEFORE UPDATE ON `business` FOR EACH ROW BEGIN
    IF ST_Distance_Sphere(
        POINT(NEW.longitude, NEW.latitude),
        POINT(28.192483, -25.746400)
    ) > 1000 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Business location must be within 1 km of Richfield Pretoria Campus.';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `business_amenity`
--

DROP TABLE IF EXISTS `business_amenity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `business_amenity` (
  `business_id` int unsigned NOT NULL,
  `amenity_id` int unsigned NOT NULL,
  PRIMARY KEY (`business_id`,`amenity_id`),
  KEY `fk_business_amenity_amenity` (`amenity_id`),
  CONSTRAINT `fk_business_amenity_amenity` FOREIGN KEY (`amenity_id`) REFERENCES `amenity` (`amenity_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_business_amenity_business` FOREIGN KEY (`business_id`) REFERENCES `business` (`business_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `business_amenity`
--

LOCK TABLES `business_amenity` WRITE;
/*!40000 ALTER TABLE `business_amenity` DISABLE KEYS */;
INSERT INTO `business_amenity` VALUES (3,1);
/*!40000 ALTER TABLE `business_amenity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `business_hours`
--

DROP TABLE IF EXISTS `business_hours`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `business_hours` (
  `business_hours_id` int unsigned NOT NULL AUTO_INCREMENT,
  `business_id` int unsigned NOT NULL,
  `day_of_week` enum('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
  `open_time` time DEFAULT NULL,
  `close_time` time DEFAULT NULL,
  `is_closed` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`business_hours_id`),
  UNIQUE KEY `uq_business_day` (`business_id`,`day_of_week`),
  KEY `idx_business_hours_business` (`business_id`),
  CONSTRAINT `fk_business_hours_business` FOREIGN KEY (`business_id`) REFERENCES `business` (`business_id`) ON DELETE CASCADE,
  CONSTRAINT `chk_business_hours_time` CHECK (((`is_closed` = true) or ((`open_time` is not null) and (`close_time` is not null))))
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `business_hours`
--

LOCK TABLES `business_hours` WRITE;
/*!40000 ALTER TABLE `business_hours` DISABLE KEYS */;
INSERT INTO `business_hours` VALUES (1,1,'MONDAY','08:30:00','18:00:00',0),(2,1,'TUESDAY','08:30:00','18:00:00',0),(3,1,'WEDNESDAY','08:30:00','18:00:00',0),(4,1,'THURSDAY','08:30:00','18:00:00',0),(5,1,'FRIDAY','08:30:00','18:00:00',0),(6,1,'SATURDAY','08:30:00','16:30:00',0),(7,1,'SUNDAY','09:00:00','15:00:00',0),(8,2,'MONDAY','07:00:00','22:00:00',0),(9,2,'TUESDAY','07:00:00','22:00:00',0),(10,2,'WEDNESDAY','07:00:00','22:00:00',0),(11,2,'THURSDAY','07:00:00','22:00:00',0),(12,2,'FRIDAY','07:00:00','22:00:00',0),(13,2,'SATURDAY','07:00:00','22:00:00',0),(14,2,'SUNDAY','07:00:00','22:00:00',0),(15,3,'MONDAY','09:00:00','17:00:00',0),(16,3,'TUESDAY','09:00:00','17:00:00',0),(17,3,'WEDNESDAY','09:00:00','17:00:00',0),(18,3,'THURSDAY','09:00:00','17:00:00',0),(19,3,'FRIDAY','09:00:00','17:00:00',0),(20,3,'SATURDAY','08:00:00','12:50:00',0),(21,3,'SUNDAY',NULL,NULL,1);
/*!40000 ALTER TABLE `business_hours` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `business_image`
--

DROP TABLE IF EXISTS `business_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `business_image` (
  `image_id` int unsigned NOT NULL AUTO_INCREMENT,
  `business_id` int unsigned NOT NULL,
  `image_url` varchar(500) NOT NULL,
  `is_primary` tinyint(1) NOT NULL DEFAULT '0',
  `uploaded_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`image_id`),
  KEY `idx_business_image_business` (`business_id`),
  CONSTRAINT `fk_business_image_business` FOREIGN KEY (`business_id`) REFERENCES `business` (`business_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `business_image`
--

LOCK TABLES `business_image` WRITE;
/*!40000 ALTER TABLE `business_image` DISABLE KEYS */;
/*!40000 ALTER TABLE `business_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `category_id` int unsigned NOT NULL AUTO_INCREMENT,
  `category_name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `category_name` (`category_name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `category`
--

LOCK TABLES `category` WRITE;
/*!40000 ALTER TABLE `category` DISABLE KEYS */;
INSERT INTO `category` VALUES (1,'Food & Drinks','Restaurants, cafes, takeaways and food outlets',1),(2,'Sit-Down Eateries','Restaurants and places for sit-down dining',1),(3,'Salons & Personal Care','Hair salons, barbers and personal care services',1),(4,'Study & Social Spaces','Study areas, cafes and social spaces with useful facilities',1),(5,'Events & Entertainment','Events, entertainment and recreational activities',1);
/*!40000 ALTER TABLE `category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `event`
--

DROP TABLE IF EXISTS `event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event` (
  `event_id` int unsigned NOT NULL AUTO_INCREMENT,
  `business_id` int unsigned NOT NULL,
  `title` varchar(150) NOT NULL,
  `venue` varchar(100) NOT NULL,
  `description` text,
  `event_date` date NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time DEFAULT NULL,
  `ticket_price` decimal(10,2) unsigned DEFAULT '0.00',
  `status` enum('UPCOMING','ONGOING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'UPCOMING',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`),
  KEY `idx_event_business` (`business_id`),
  KEY `idx_event_date` (`event_date`),
  CONSTRAINT `fk_event_business` FOREIGN KEY (`business_id`) REFERENCES `business` (`business_id`) ON DELETE CASCADE,
  CONSTRAINT `chk_event_times` CHECK (((`end_time` is null) or (`end_time` > `start_time`)))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event`
--

LOCK TABLES `event` WRITE;
/*!40000 ALTER TABLE `event` DISABLE KEYS */;
/*!40000 ALTER TABLE `event` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_event_future_insert` BEFORE INSERT ON `event` FOR EACH ROW BEGIN
    IF TIMESTAMP(NEW.event_date, NEW.start_time) <= CURRENT_TIMESTAMP THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Event start date and time must be in the future.';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_event_future_update` BEFORE UPDATE ON `event` FOR EACH ROW BEGIN
    IF NEW.status = 'UPCOMING'
       AND TIMESTAMP(NEW.event_date, NEW.start_time) <= CURRENT_TIMESTAMP THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'An upcoming event must have a future start date and time.';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `review`
--

DROP TABLE IF EXISTS `review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
  `review_id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `business_id` int unsigned NOT NULL,
  `rating` tinyint unsigned NOT NULL,
  `comment` text,
  `is_verified` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`review_id`),
  KEY `idx_review_business` (`business_id`),
  KEY `idx_review_user` (`user_id`),
  CONSTRAINT `fk_review_business` FOREIGN KEY (`business_id`) REFERENCES `business` (`business_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_review_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `chk_review_rating` CHECK ((`rating` between 1 and 5))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review`
--

LOCK TABLES `review` WRITE;
/*!40000 ALTER TABLE `review` DISABLE KEYS */;
/*!40000 ALTER TABLE `review` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_review_no_self_review` BEFORE INSERT ON `review` FOR EACH ROW BEGIN
    DECLARE v_owner_id INT UNSIGNED;

    SELECT owner_id
    INTO v_owner_id
    FROM business
    WHERE business_id = NEW.business_id;

    IF NEW.user_id = v_owner_id THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Business owners cannot review their own business.';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_review_24h_limit` BEFORE INSERT ON `review` FOR EACH ROW BEGIN
    IF EXISTS (
        SELECT 1
        FROM review AS r
        WHERE r.user_id = NEW.user_id
          AND r.business_id = NEW.business_id
          AND r.created_at >= CURRENT_TIMESTAMP - INTERVAL 24 HOUR
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'You cannot review the same business more than once within 24 hours.';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `user_id` int unsigned NOT NULL AUTO_INCREMENT,
  `full_name` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('USER','OWNER','ADMIN') NOT NULL DEFAULT 'USER',
  `account_status` enum('ACTIVE','INACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'Slondiwe Xulu','Slo.owner@cbdventures.co.za','TEST_HASH_OWNER_001','OWNER','ACTIVE','2026-10-02 19:13:53','2026-10-02 19:13:53'),(2,'Peta','peta.owner@cbdventures.co.za','TEST_HASH_OWNER_002','OWNER','ACTIVE','2026-10-02 19:13:53','2026-10-02 19:13:53'),(3,'Kamo User','kamo.user@cbdventures.co.za','TEST_HASH_USER_001','USER','ACTIVE','2026-10-02 19:13:53','2026-10-02 19:13:53'),(4,'CBD Admin','admin@cbdventures.co.za','TEST_HASH_ADMIN_001','ADMIN','ACTIVE','2026-10-02 19:13:53','2026-10-02 19:13:53');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_activity`
--

DROP TABLE IF EXISTS `user_activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_activity` (
  `activity_id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `business_id` int unsigned DEFAULT NULL,
  `event_id` int unsigned DEFAULT NULL,
  `activity_type` varchar(50) NOT NULL,
  `occurred_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`activity_id`),
  KEY `idx_activity_user` (`user_id`),
  KEY `idx_activity_business` (`business_id`),
  KEY `idx_activity_event` (`event_id`),
  CONSTRAINT `fk_user_activity_business` FOREIGN KEY (`business_id`) REFERENCES `business` (`business_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_user_activity_event` FOREIGN KEY (`event_id`) REFERENCES `event` (`event_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_user_activity_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_activity`
--

LOCK TABLES `user_activity` WRITE;
/*!40000 ALTER TABLE `user_activity` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_activity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_preference`
--

DROP TABLE IF EXISTS `user_preference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_preference` (
  `preference_id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `preferred_price_level` tinyint unsigned DEFAULT NULL,
  `max_distance_km` decimal(5,2) DEFAULT NULL,
  `wifi_required` tinyint(1) NOT NULL DEFAULT '0',
  `power_required` tinyint(1) NOT NULL DEFAULT '0',
  `noise_level_preference` enum('QUIET','MODERATE','ANY') DEFAULT 'ANY',
  `student_discount_required` tinyint(1) NOT NULL DEFAULT '0',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`preference_id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `fk_user_preference_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `chk_preference_distance` CHECK (((`max_distance_km` is null) or (`max_distance_km` > 0))),
  CONSTRAINT `chk_preference_price` CHECK (((`preferred_price_level` is null) or (`preferred_price_level` between 1 and 3)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_preference`
--

LOCK TABLES `user_preference` WRITE;
/*!40000 ALTER TABLE `user_preference` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_preference` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_preference_category`
--

DROP TABLE IF EXISTS `user_preference_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_preference_category` (
  `preference_id` int unsigned NOT NULL,
  `category_id` int unsigned NOT NULL,
  PRIMARY KEY (`preference_id`,`category_id`),
  KEY `fk_preference_category_category` (`category_id`),
  CONSTRAINT `fk_preference_category_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_preference_category_preference` FOREIGN KEY (`preference_id`) REFERENCES `user_preference` (`preference_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_preference_category`
--

LOCK TABLES `user_preference_category` WRITE;
/*!40000 ALTER TABLE `user_preference_category` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_preference_category` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-20  9:08:20
