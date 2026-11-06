-- Tables: amenity
CREATE TABLE `amenity` (
  `amenity_id` int unsigned NOT NULL AUTO_INCREMENT,
  `amenity_name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`amenity_id`),
  UNIQUE KEY `amenity_name` (`amenity_name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- Insert batch #1
INSERT INTO amenity (amenity_id, amenity_name, description, is_active) VALUES
(1, 'Free Wi-Fi', 'Wireless internet access for customers', 1),
(2, 'Power Outlets', 'Electrical power outlets available for customers', 1),
(3, 'Quiet Environment', 'Suitable for studying or quiet work', 1),
(4, 'Student Discount', 'Discount or special pricing for students', 1),
(5, 'Backup Power', 'Alternative power available during electricity interruptions', 1);

-- Tables: category
CREATE TABLE `category` (
  `category_id` int unsigned NOT NULL AUTO_INCREMENT,
  `category_name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `category_name` (`category_name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- Insert batch #1
INSERT INTO category (category_id, category_name, description, is_active) VALUES
(1, 'Food & Drinks', 'Restaurants, cafes, takeaways and food outlets', 1),
(2, 'Sit-Down Eateries', 'Restaurants and places for sit-down dining', 1),
(3, 'Salons & Personal Care', 'Hair salons, barbers and personal care services', 1),
(4, 'Study & Social Spaces', 'Study areas, cafes and social spaces with useful facilities', 1),
(5, 'Events & Entertainment', 'Events, entertainment and recreational activities', 1);

-- Tables: user
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
-- Insert batch #1
INSERT INTO user (user_id, full_name, email, password_hash, role, account_status, created_at, updated_at) VALUES
(1, 'Slondiwe Xulu', 'Slo.owner@cbdventures.co.za', 'TEST_HASH_OWNER_001', 'OWNER', 'ACTIVE', 10/2/2026 9:13:53 PM, 10/2/2026 9:13:53 PM),
(2, 'Peta', 'peta.owner@cbdventures.co.za', 'TEST_HASH_OWNER_002', 'OWNER', 'ACTIVE', 10/2/2026 9:13:53 PM, 10/2/2026 9:13:53 PM),
(3, 'Kamo User', 'kamo.user@cbdventures.co.za', 'TEST_HASH_USER_001', 'USER', 'ACTIVE', 10/2/2026 9:13:53 PM, 10/2/2026 9:13:53 PM),
(4, 'CBD Admin', 'admin@cbdventures.co.za', 'TEST_HASH_ADMIN_001', 'ADMIN', 'ACTIVE', 10/2/2026 9:13:53 PM, 10/2/2026 9:13:53 PM);

-- Tables: business
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
-- Insert batch #1
INSERT INTO business (business_id, owner_id, category_id, name, description, address, latitude, longitude, contact_phone, contact_email, price_level, status, created_at, updated_at) VALUES
(1, NULL, 3, 'SA Queen Hair & Beauty', 'Hair and beauty salon offering hair and nail services.', '231 Church St, Pretoria Central, Pretoria, 0002, South Africa', -25.7465700, 28.1892000, '072 676 2797', NULL, NULL, 'APPROVED', 10/2/2026 9:15:56 PM, 10/13/2026 11:20:19 PM),
(2, NULL, 2, 'Black Bistro Lounge & Cocktail Kitchen', 'Bistro and cocktail kitchen serving meals and drinks at the South African State Theatre.', 'Drama Theatre Entrance, 320 Pretorius St, Pretoria Central, 0002, South Africa', -25.7467600, 28.1947200, '+27 12 320 0206', NULL, NULL, 'APPROVED', 10/2/2026 9:15:56 PM, 10/13/2026 11:50:47 PM),
(3, NULL, 4, 'Es''kia Mphahlele Community Library', 'Public library providing reading, research and information services.', '347 Madiba St, Pretoria Central, Pretoria, 0002, South Africa', -25.7446296, 28.1948915, '+27 12 358 8957', NULL, NULL, 'APPROVED', 10/2/2026 9:15:56 PM, 10/13/2026 11:27:09 PM);

-- Tables: business_amenity
CREATE TABLE `business_amenity` (
  `business_id` int unsigned NOT NULL,
  `amenity_id` int unsigned NOT NULL,
  PRIMARY KEY (`business_id`,`amenity_id`),
  KEY `fk_business_amenity_amenity` (`amenity_id`),
  CONSTRAINT `fk_business_amenity_amenity` FOREIGN KEY (`amenity_id`) REFERENCES `amenity` (`amenity_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_business_amenity_business` FOREIGN KEY (`business_id`) REFERENCES `business` (`business_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- Insert batch #1
INSERT INTO business_amenity (business_id, amenity_id) VALUES
(3, 1);

-- Tables: business_hours
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
-- Insert batch #1
INSERT INTO business_hours (business_hours_id, business_id, day_of_week, open_time, close_time, is_closed) VALUES
(1, 1, 'MONDAY', '08:30:00', '18:00:00', 0),
(2, 1, 'TUESDAY', '08:30:00', '18:00:00', 0),
(3, 1, 'WEDNESDAY', '08:30:00', '18:00:00', 0),
(4, 1, 'THURSDAY', '08:30:00', '18:00:00', 0),
(5, 1, 'FRIDAY', '08:30:00', '18:00:00', 0),
(6, 1, 'SATURDAY', '08:30:00', '16:30:00', 0),
(7, 1, 'SUNDAY', '09:00:00', '15:00:00', 0),
(8, 2, 'MONDAY', '07:00:00', '22:00:00', 0),
(9, 2, 'TUESDAY', '07:00:00', '22:00:00', 0),
(10, 2, 'WEDNESDAY', '07:00:00', '22:00:00', 0),
(11, 2, 'THURSDAY', '07:00:00', '22:00:00', 0),
(12, 2, 'FRIDAY', '07:00:00', '22:00:00', 0),
(13, 2, 'SATURDAY', '07:00:00', '22:00:00', 0),
(14, 2, 'SUNDAY', '07:00:00', '22:00:00', 0),
(15, 3, 'MONDAY', '09:00:00', '17:00:00', 0),
(16, 3, 'TUESDAY', '09:00:00', '17:00:00', 0),
(17, 3, 'WEDNESDAY', '09:00:00', '17:00:00', 0),
(18, 3, 'THURSDAY', '09:00:00', '17:00:00', 0),
(19, 3, 'FRIDAY', '09:00:00', '17:00:00', 0),
(20, 3, 'SATURDAY', '08:00:00', '12:50:00', 0),
(21, 3, 'SUNDAY', NULL, NULL, 1);

-- Tables: business_image
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

-- Tables: event
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Tables: review
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Tables: user_activity
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

-- Tables: user_preference
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

-- Tables: user_preference_category
CREATE TABLE `user_preference_category` (
  `preference_id` int unsigned NOT NULL,
  `category_id` int unsigned NOT NULL,
  PRIMARY KEY (`preference_id`,`category_id`),
  KEY `fk_preference_category_category` (`category_id`),
  CONSTRAINT `fk_preference_category_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_preference_category_preference` FOREIGN KEY (`preference_id`) REFERENCES `user_preference` (`preference_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

