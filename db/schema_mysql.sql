-- =====================================================================
--  CBDVentures — Physical MySQL 8 schema
--  IT Project Group 5 · Phase 3 (Implementation) · Step 5
--
--  Mirrors the JPA entities one-to-one. Run with the "mysql" profile:
--    mysql -u root -p < db/schema_mysql.sql
--    DB_USER=cbdventures DB_PASSWORD=... ./mvnw spring-boot:run \
--        -Dspring-boot.run.profiles=mysql
--
--  Design notes
--   * BIGINT surrogate primary keys (Phase 3 note: INT in the visual
--     design was implemented as BIGINT for scalability).
--   * DECIMAL(10,8) / DECIMAL(11,8) for GPS coordinates.
--   * ENUM columns for the closed value sets (role, price level, status, zone).
--   * RESTRICT on master data (user as owner, category) and CASCADE on
--     dependent child tables (reviews, images, events, amenities).
--   * 3NF: categories, amenities and users are referenced, never duplicated.
--   * Opening hours are normalised into business_hours (one row per business
--     per weekday) so "closed on Sundays" and "is it open now?" are answerable.
--     business.open_time/close_time remain as a documented fallback for
--     listings that only ever traded in one window.
--   * user_activity stores the browsing trail used by the recommendation
--     engine (FR-04). It is deliberately a weak reference: deleting a listing
--     nulls the link, deleting a user removes the trail.
--   * Trigger-level rules (1 km zone, future events, no self-review, review
--     edit limit) live in db/triggers_mysql.sql so the rules hold even when rows
--     are changed directly in SQL rather than through the application.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS cbdventures
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE cbdventures;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS user_activity;
DROP TABLE IF EXISTS business_hours;
DROP TABLE IF EXISTS preference_category;
DROP TABLE IF EXISTS user_preference;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS event;
DROP TABLE IF EXISTS business_image;
DROP TABLE IF EXISTS business_amenity;
DROP TABLE IF EXISTS business;
DROP TABLE IF EXISTS amenity;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS app_user;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- USER
-- ---------------------------------------------------------------------
CREATE TABLE app_user (
    user_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    full_name     VARCHAR(100) NOT NULL COMMENT 'Full name of the registered user',
    email         VARCHAR(150) NOT NULL COMMENT 'Unique login email address',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt hash — never plain text',
    role          ENUM('VISITOR','OWNER','ADMIN') NOT NULL DEFAULT 'VISITOR',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_email (email),
    CONSTRAINT chk_user_email CHECK (email LIKE '%_@_%._%'),
    CONSTRAINT chk_user_name  CHECK (CHAR_LENGTH(TRIM(full_name)) > 1)
) ENGINE = InnoDB COMMENT = 'Students, visitors, business owners and administrators';

-- ---------------------------------------------------------------------
-- CATEGORY — the five approved discovery classifications
-- ---------------------------------------------------------------------
CREATE TABLE category (
    category_id   BIGINT      NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(80) NOT NULL,
    slug          VARCHAR(80) NOT NULL COMMENT 'URL-safe identifier used for filtering',
    description   VARCHAR(255) NULL,
    icon_key      VARCHAR(40)  NULL COMMENT 'Icon token resolved by the web client',
    sort_order    INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_category_slug (slug),
    UNIQUE KEY uk_category_name (category_name)
) ENGINE = InnoDB COMMENT = 'Curated categories (adding a category is data, not code)';

-- ---------------------------------------------------------------------
-- AMENITY (reference data; the junction table follows BUSINESS)
-- ---------------------------------------------------------------------
CREATE TABLE amenity (
    amenity_id   BIGINT      NOT NULL AUTO_INCREMENT,
    category_id  BIGINT      NULL COMMENT 'Optional grouping category',
    amenity_name VARCHAR(50) NOT NULL,
    icon_key     VARCHAR(40) NULL,
    PRIMARY KEY (amenity_id),
    UNIQUE KEY uk_amenity_name (amenity_name),
    KEY idx_amenity_category (category_id),
    CONSTRAINT fk_amenity_category FOREIGN KEY (category_id)
        REFERENCES category (category_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- BUSINESS
-- ---------------------------------------------------------------------
CREATE TABLE business (
    business_id     BIGINT         NOT NULL AUTO_INCREMENT,
    owner_id        BIGINT         NOT NULL COMMENT 'FK app_user.user_id (OWNER role)',
    category_id     BIGINT         NOT NULL COMMENT 'FK category.category_id',
    name            VARCHAR(120)   NOT NULL,
    description     TEXT           NULL,
    address         VARCHAR(255)   NOT NULL,
    latitude        DECIMAL(10,8)  NOT NULL COMMENT 'WGS84 latitude (-90..90)',
    longitude       DECIMAL(11,8)  NOT NULL COMMENT 'WGS84 longitude (-180..180)',
    zone_radius     ENUM('PRIMARY_1KM','EXTENDED_5KM') NOT NULL DEFAULT 'PRIMARY_1KM',
    price_level     ENUM('R','RR','RRR') NOT NULL DEFAULT 'R',
    operating_hours VARCHAR(120)   NULL COMMENT 'Display string, e.g. Mon-Sat 07:00 - 17:00',
    open_time       TIME           NULL COMMENT 'Structured opening time (with close_time)',
    close_time      TIME           NULL,
    phone           VARCHAR(40)    NULL,
    whatsapp        VARCHAR(40)    NULL,
    website         VARCHAR(200)   NULL,
    status          ENUM('PENDING','ACTIVE','REJECTED','INACTIVE') NOT NULL DEFAULT 'PENDING',
    admin_note      VARCHAR(255)   NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (business_id),
    KEY idx_business_status (status),
    KEY idx_business_category (category_id),
    KEY idx_business_owner (owner_id),
    KEY idx_business_name (name),
    KEY idx_business_location (latitude, longitude),
    CONSTRAINT fk_business_owner FOREIGN KEY (owner_id)
        REFERENCES app_user (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_business_category FOREIGN KEY (category_id)
        REFERENCES category (category_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_business_lat CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_business_lng CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_business_hours CHECK (
        (open_time IS NULL AND close_time IS NULL) OR
        (open_time IS NOT NULL AND close_time IS NOT NULL AND open_time <> close_time))
) ENGINE = InnoDB COMMENT = 'Business listings inside the pilot zone';

-- BUSINESS_AMENITY junction (created after BUSINESS: MySQL requires the
-- referenced table to exist first)
-- ---------------------------------------------------------------------
CREATE TABLE business_amenity (
    business_id BIGINT NOT NULL,
    amenity_id  BIGINT NOT NULL,
    PRIMARY KEY (business_id, amenity_id),
    KEY idx_ba_amenity (amenity_id),
    CONSTRAINT fk_ba_business FOREIGN KEY (business_id)
        REFERENCES business (business_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ba_amenity FOREIGN KEY (amenity_id)
        REFERENCES amenity (amenity_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- BUSINESS_IMAGE
-- ---------------------------------------------------------------------
CREATE TABLE business_image (
    image_id    BIGINT       NOT NULL AUTO_INCREMENT,
    business_id BIGINT       NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    caption     VARCHAR(160) NULL,
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'Exactly one primary thumbnail per business',
    PRIMARY KEY (image_id),
    KEY idx_image_business (business_id),
    CONSTRAINT fk_image_business FOREIGN KEY (business_id)
        REFERENCES business (business_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- BUSINESS_HOURS — one row per business per weekday (3NF: no repeating groups)
-- ---------------------------------------------------------------------
CREATE TABLE business_hours (
    hours_id    BIGINT   NOT NULL AUTO_INCREMENT,
    business_id BIGINT   NOT NULL,
    day_of_week TINYINT  NOT NULL COMMENT 'ISO weekday: 1 = Monday ... 7 = Sunday',
    is_closed   BOOLEAN  NOT NULL DEFAULT FALSE,
    open_time   TIME     NULL,
    close_time  TIME     NULL,
    PRIMARY KEY (hours_id),
    UNIQUE KEY uk_business_hours_day (business_id, day_of_week),
    CONSTRAINT fk_hours_business FOREIGN KEY (business_id)
        REFERENCES business (business_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_hours_day CHECK (day_of_week BETWEEN 1 AND 7),
    -- A closed day stores no times; an open day stores both, opening first.
    CONSTRAINT chk_hours_window CHECK (
        (is_closed = TRUE AND open_time IS NULL AND close_time IS NULL) OR
        (is_closed = FALSE AND open_time IS NOT NULL AND close_time IS NOT NULL
         AND open_time < close_time))
) ENGINE = InnoDB COMMENT = 'Opening hours per weekday; authoritative for "open now"';

-- ---------------------------------------------------------------------
-- USER_ACTIVITY — browsing trail behind personalised suggestions (FR-04)
-- ---------------------------------------------------------------------
CREATE TABLE user_activity (
    activity_id   BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT      NOT NULL,
    business_id   BIGINT      NULL COMMENT 'NULL for activity not tied to a listing',
    activity_type ENUM('VIEW_PROFILE','SEARCH','SAVE_BUSINESS') NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (activity_id),
    KEY idx_activity_user (user_id, created_at),
    KEY idx_activity_business (business_id),
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id)
        REFERENCES app_user (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    -- ON DELETE SET NULL keeps the aggregate interest history when a listing leaves.
    CONSTRAINT fk_activity_business FOREIGN KEY (business_id)
        REFERENCES business (business_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB COMMENT = 'What a signed-in visitor viewed, searched or saved';

-- ---------------------------------------------------------------------
-- EVENT — always attached to a venue (business)
-- ---------------------------------------------------------------------
CREATE TABLE event (
    event_id        BIGINT        NOT NULL AUTO_INCREMENT,
    business_id     BIGINT        NOT NULL,
    title           VARCHAR(100)  NOT NULL,
    description     TEXT          NULL,
    venue           VARCHAR(100)  NULL COMMENT 'Room or area inside the venue',
    event_date_time DATETIME      NOT NULL,
    ticket_price    DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '0.00 represents a free event',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id),
    KEY idx_event_datetime (event_date_time),
    KEY idx_event_business (business_id),
    CONSTRAINT fk_event_business FOREIGN KEY (business_id)
        REFERENCES business (business_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_event_price CHECK (ticket_price >= 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- REVIEW — one active review per user per business
-- ---------------------------------------------------------------------
CREATE TABLE review (
    review_id   BIGINT    NOT NULL AUTO_INCREMENT,
    user_id     BIGINT    NOT NULL,
    business_id BIGINT    NOT NULL,
    rating      INT       NOT NULL COMMENT 'Whole number 1..5',
    comment     TEXT      NULL,
    is_verified BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id),
    UNIQUE KEY uk_review_user_business (user_id, business_id),
    KEY idx_review_business (business_id),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id)
        REFERENCES app_user (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_review_business FOREIGN KEY (business_id)
        REFERENCES business (business_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_review_comment CHECK (comment IS NULL OR CHAR_LENGTH(comment) <= 1000)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- USER_PREFERENCE + PREFERENCE_CATEGORY (FR-04 personalisation)
-- ---------------------------------------------------------------------
CREATE TABLE user_preference (
    preference_id    BIGINT      NOT NULL AUTO_INCREMENT,
    user_id          BIGINT      NOT NULL,
    max_price_level  ENUM('R','RR','RRR') NULL,
    max_distance_km  DOUBLE      NULL,
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (preference_id),
    UNIQUE KEY uk_preference_user (user_id),
    CONSTRAINT fk_preference_user FOREIGN KEY (user_id)
        REFERENCES app_user (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_preference_distance CHECK (max_distance_km IS NULL OR max_distance_km > 0)
) ENGINE = InnoDB;

CREATE TABLE preference_category (
    preference_id BIGINT NOT NULL,
    category_id   BIGINT NOT NULL,
    PRIMARY KEY (preference_id, category_id),
    KEY idx_pc_category (category_id),
    CONSTRAINT fk_pc_preference FOREIGN KEY (preference_id)
        REFERENCES user_preference (preference_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pc_category FOREIGN KEY (category_id)
        REFERENCES category (category_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =====================================================================
--  REFERENCE DATA — the five approved categories and standard amenities
-- =====================================================================
INSERT INTO category (category_name, slug, description, icon_key, sort_order) VALUES
 ('Food & Drinks', 'food-drinks',
  'Kotas, shawarmas, breakfast sandwiches, coffee carts and everyday quick eats.', 'food', 1),
 ('Restaurants & Sit-Down Eateries', 'restaurants',
  'Formal dining, food halls such as The Containers, and sit-down spots worth the walk.', 'restaurant', 2),
 ('Salons & Personal Care', 'salons',
  'Barbershops, hair salons, nail studios and beauty bars within walking distance.', 'salon', 3),
 ('Study & Social Spaces', 'study-spaces',
  'Cafés, libraries and co-working corners filtered by Wi-Fi, power and quiet.', 'study', 4),
 ('Events & Entertainment', 'events',
  'State Theatre performances, markets, pop-ups and local happenings in the zone.', 'event', 5);

INSERT INTO amenity (category_id, amenity_name, icon_key)
SELECT category_id, 'Free Wi-Fi', 'wifi' FROM category WHERE slug = 'food-drinks';
INSERT INTO amenity (category_id, amenity_name, icon_key)
SELECT category_id, 'Backup Power', 'power' FROM category WHERE slug = 'food-drinks';
INSERT INTO amenity (category_id, amenity_name, icon_key)
SELECT category_id, 'Power Outlets', 'plug' FROM category WHERE slug = 'study-spaces';
INSERT INTO amenity (category_id, amenity_name, icon_key)
SELECT category_id, 'Quiet Atmosphere', 'quiet' FROM category WHERE slug = 'study-spaces';
INSERT INTO amenity (category_id, amenity_name, icon_key)
SELECT category_id, 'Student Specials', 'student' FROM category WHERE slug = 'food-drinks';
INSERT INTO amenity (category_id, amenity_name, icon_key)
SELECT category_id, 'Card Payments', 'card' FROM category WHERE slug = 'restaurants';
INSERT INTO amenity (category_id, amenity_name, icon_key)
SELECT category_id, 'Cash Only', 'cash' FROM category WHERE slug = 'food-drinks';
INSERT INTO amenity (category_id, amenity_name, icon_key)
SELECT category_id, 'Takeaway', 'takeaway' FROM category WHERE slug = 'food-drinks';
INSERT INTO amenity (category_id, amenity_name, icon_key)
SELECT category_id, 'Outdoor Seating', 'outdoor' FROM category WHERE slug = 'restaurants';
INSERT INTO amenity (category_id, amenity_name, icon_key)
SELECT category_id, 'Air Conditioning', 'aircon' FROM category WHERE slug = 'restaurants';
INSERT INTO amenity (category_id, amenity_name, icon_key)
SELECT category_id, 'Wheelchair Accessible', 'accessible' FROM category WHERE slug = 'events';
INSERT INTO amenity (category_id, amenity_name, icon_key)
SELECT category_id, 'Parking Nearby', 'parking' FROM category WHERE slug = 'events';
INSERT INTO amenity (category_id, amenity_name, icon_key)
SELECT category_id, 'Generous Portions', 'portion' FROM category WHERE slug = 'food-drinks';
INSERT INTO amenity (category_id, amenity_name, icon_key)
SELECT category_id, 'Late Night', 'late' FROM category WHERE slug = 'events';

-- The full demo data set (26 listings, 45 reviews, 12 events, 17 accounts) is
-- inserted by the Spring Boot DataSeeder on first start, so the same seed logic
-- is used for H2 and MySQL. Passwords are BCrypt hashed at runtime; the demo
-- password for seeded accounts is "Password123!" and "Admin123!" for the admin.

-- =====================================================================
--  VERIFICATION QUERIES (used during Phase 4 integration testing)
-- =====================================================================
-- 1. Every business references a valid owner and category.
-- SELECT b.business_id, b.name FROM business b
--   LEFT JOIN app_user u ON u.user_id = b.owner_id
--   LEFT JOIN category c ON c.category_id = b.category_id
--   WHERE u.user_id IS NULL OR c.category_id IS NULL;
--
-- 2. Average rating per business (the same aggregate the API exposes).
-- SELECT b.business_id, b.name, ROUND(AVG(r.rating), 1) AS avg_rating, COUNT(r.review_id) AS reviews
--   FROM business b LEFT JOIN review r ON r.business_id = b.business_id
--   GROUP BY b.business_id, b.name ORDER BY avg_rating DESC;
--
-- 3. No duplicate reviews per user/business (unique index enforces this).
-- SELECT user_id, business_id, COUNT(*) FROM review GROUP BY user_id, business_id HAVING COUNT(*) > 1;
--
-- 4. Listings outside the 1 km pilot zone (haversine, anchor = Richfield).
-- SELECT business_id, name,
--   ROUND(6371 * ACOS(LEAST(1, COS(RADIANS(-25.746350)) * COS(RADIANS(latitude))
--        * COS(RADIANS(longitude) - RADIANS(28.192550))
--        + SIN(RADIANS(-25.746350)) * SIN(RADIANS(latitude)))), 3) AS distance_km
--   FROM business HAVING distance_km > 1 ORDER BY distance_km DESC;

-- 5. Business hours completeness: every ACTIVE listing should have seven rows.
-- SELECT b.business_id, b.name, COUNT(h.hours_id) AS days
--   FROM business b LEFT JOIN business_hours h ON h.business_id = b.business_id
--   WHERE b.status = 'ACTIVE'
--   GROUP BY b.business_id, b.name HAVING days <> 7;
--
-- 6. Which listings are open right now (per-day hours), using server time.
-- SELECT b.name, h.open_time, h.close_time FROM business b
--   JOIN business_hours h ON h.business_id = b.business_id
--     AND h.day_of_week = WEEKDAY(CURDATE()) + 1
--   WHERE h.is_closed = FALSE AND CURTIME() BETWEEN h.open_time AND h.close_time;
--
-- 7. Personalisation signal: activity volume per category.
-- SELECT c.category_name, a.activity_type, COUNT(*) AS events
--   FROM user_activity a
--   JOIN business b ON b.business_id = a.business_id
--   JOIN category c ON c.category_id = b.category_id
--   GROUP BY c.category_name, a.activity_type ORDER BY events DESC;
--
-- 8. Trigger inventory (expected: 8 triggers after db/triggers_mysql.sql).
-- SELECT TRIGGER_NAME, EVENT_MANIPULATION, EVENT_OBJECT_TABLE
--   FROM information_schema.TRIGGERS
--   WHERE TRIGGER_SCHEMA = 'cbdventures' ORDER BY EVENT_OBJECT_TABLE, ACTION_TIMING;
