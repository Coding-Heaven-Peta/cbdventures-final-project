-- =====================================================================
--  CBDVentures — database rule tests
--  IT Project Group 5 · Phase 4 (Testing)
--
--  Every test runs inside a transaction that is rolled back, so the database
--  is left exactly as it was found. Expected failures are asserted by hand:
--  the point of these tests is that the *database* refuses the row, which is
--  why the checks are written as statements with a comment saying which error
--  is expected.
--
--  Usage (--force is required: the expected errors below must not stop the run):
--    mysql -u root -p --force cbdventures < db/tests/trigger_tests.sql
--
--  Pass criterion: the closing banner "ALL TESTS RUN" appears, every error line
--  names the rule it should name, and the two post-condition counts are 0.
--
--  Reference point (Richfield campus, Pretoria CBD): -25.746350, 28.192550
-- =====================================================================

USE cbdventures;

-- Fixtures: one owner, one visitor, one category, one valid listing.
-- IDs are captured in variables so the tests do not depend on auto-increment
-- values left behind by earlier runs.

START TRANSACTION;

INSERT INTO app_user (full_name, email, password_hash, role)
VALUES ('Trigger Owner', 'triggers.owner@cbdventures.test',
        '$2a$10$0000000000000000000000000000000000000000000000000000', 'OWNER');
SET @owner := LAST_INSERT_ID();

INSERT INTO app_user (full_name, email, password_hash, role)
VALUES ('Trigger Visitor', 'triggers.visitor@cbdventures.test',
        '$2a$10$0000000000000000000000000000000000000000000000000000', 'VISITOR');
SET @visitor := LAST_INSERT_ID();

SELECT category_id INTO @category FROM category ORDER BY sort_order LIMIT 1;

-- A listing 191.96 m from the anchor: inside the 1 km zone.
INSERT INTO business (owner_id, category_id, name, address, latitude, longitude,
                      zone_radius, price_level, status)
VALUES (@owner, @category, 'Zone Test Kitchen', '1 Test Street, Pretoria Central',
        -25.745991, 28.194345, 'PRIMARY_1KM', 'R', 'ACTIVE');
SET @business := LAST_INSERT_ID();

SELECT '--- SETUP: fixtures created ---' AS step;

-- ---------------------------------------------------------------------
SELECT 'TEST 1 - valid listing inside the zone was accepted' AS test;
SELECT business_id, name FROM business WHERE business_id = @business;

-- ---------------------------------------------------------------------
SELECT 'TEST 2 - EXPECT ERROR 1644: listing 3 km away as PRIMARY_1KM' AS test;
SELECT 'TR-01 zone trigger' AS expected;
INSERT INTO business (owner_id, category_id, name, address, latitude, longitude,
                      zone_radius, price_level, status)
VALUES (@owner, @category, 'Far Away Bakery', '99 Far Road',
        -25.772000, 28.210000, 'PRIMARY_1KM', 'R', 'ACTIVE');

-- ---------------------------------------------------------------------
SELECT 'TEST 3 - EXPECT ERROR 1644: an UPDATE moving a listing out of the zone' AS test;
UPDATE business SET latitude = -25.800000, longitude = 28.250000
WHERE business_id = @business;

-- ---------------------------------------------------------------------
SELECT 'TEST 4 - the same 3 km listing IS allowed in the extended zone' AS test;
INSERT INTO business (owner_id, category_id, name, address, latitude, longitude,
                      zone_radius, price_level, status)
VALUES (@owner, @category, 'Extended Zone Eatery', '5 Middle Road',
        -25.772000, 28.210000, 'EXTENDED_5KM', 'R', 'PENDING');
SELECT business_id, name, zone_radius FROM business WHERE name = 'Extended Zone Eatery';

-- ---------------------------------------------------------------------
SELECT 'TEST 5 - hours: a full week for the listing' AS test;
INSERT INTO business_hours (business_id, day_of_week, is_closed, open_time, close_time) VALUES
  (@business, 1, FALSE, '07:00:00', '17:00:00'),
  (@business, 2, FALSE, '07:00:00', '17:00:00'),
  (@business, 3, FALSE, '07:00:00', '17:00:00'),
  (@business, 4, FALSE, '07:00:00', '17:00:00'),
  (@business, 5, FALSE, '07:00:00', '17:00:00'),
  (@business, 6, FALSE, '08:00:00', '13:00:00'),
  (@business, 7, TRUE,  NULL, NULL);
SELECT COUNT(*) AS hours_rows FROM business_hours WHERE business_id = @business;

SELECT 'TEST 6 - EXPECT ERROR 1062: the same weekday twice (unique key)' AS test;
INSERT INTO business_hours (business_id, day_of_week, is_closed, open_time, close_time)
VALUES (@business, 1, FALSE, '09:00:00', '10:00:00');

SELECT 'TEST 7 - EXPECT ERROR 1644: opening time after closing time' AS test;
INSERT INTO business_hours (business_id, day_of_week, is_closed, open_time, close_time)
VALUES (@business, 7, FALSE, '18:00:00', '09:00:00');

SELECT 'TEST 8 - EXPECT ERROR 1644: a day that is both closed and timed' AS test;
INSERT INTO business_hours (business_id, day_of_week, is_closed, open_time, close_time)
VALUES (@business, 7, TRUE, '08:00:00', '12:00:00');

SELECT 'TEST 9 - EXPECT ERROR 1452: hours for a listing that does not exist' AS test;
INSERT INTO business_hours (business_id, day_of_week, is_closed, open_time, close_time)
VALUES (999999, 1, FALSE, '08:00:00', '12:00:00');

-- ---------------------------------------------------------------------
SELECT 'TEST 10 - reviews: visitor may review the listing once' AS test;
INSERT INTO review (user_id, business_id, rating, comment)
VALUES (@visitor, @business, 5, 'Best kotas in the CBD.');
SELECT review_id, rating FROM review WHERE business_id = @business;

SELECT 'TEST 11 - EXPECT ERROR 1062: the same visitor reviewing again' AS test;
INSERT INTO review (user_id, business_id, rating, comment)
VALUES (@visitor, @business, 1, 'Changed my mind twice.');

SELECT 'TEST 12 - EXPECT ERROR 1644: the owner reviewing their own business' AS test;
INSERT INTO review (user_id, business_id, rating, comment)
VALUES (@owner, @business, 5, 'I am my own biggest fan.');

SELECT 'TEST 13 - EXPECT ERROR 4025: rating outside 1..5 (CHECK constraint)' AS test;
INSERT INTO review (user_id, business_id, rating, comment)
VALUES (@visitor, (SELECT business_id FROM business WHERE name = 'Extended Zone Eatery'), 9, 'Nine stars.');

SELECT 'TEST 14 - EXPECT ERROR 1644: editing a fresh review inside the 24 h window' AS test;
UPDATE review SET rating = 4, comment = 'Downgraded already.'
WHERE user_id = @visitor AND business_id = @business;

-- ---------------------------------------------------------------------
SELECT 'TEST 15 - events: a future event is accepted' AS test;
INSERT INTO event (business_id, title, description, event_date_time, ticket_price)
VALUES (@business, 'Trigger Test Quiz Night', 'Five rounds.', NOW() + INTERVAL 7 DAY, 40.00);
SELECT event_id, title, event_date_time FROM event WHERE title = 'Trigger Test Quiz Night';

SELECT 'TEST 16 - EXPECT ERROR 1644: an event in the past' AS test;
INSERT INTO event (business_id, title, event_date_time, ticket_price)
VALUES (@business, 'Yesterday Party', NOW() - INTERVAL 1 DAY, 0.00);

SELECT 'TEST 17 - EXPECT ERROR 1644: moving an event into the past' AS test;
UPDATE event SET event_date_time = NOW() - INTERVAL 2 DAY
WHERE title = 'Trigger Test Quiz Night';

-- ---------------------------------------------------------------------
SELECT 'TEST 18 - activity: a view is recorded and survives' AS test;
INSERT INTO user_activity (user_id, business_id, activity_type)
VALUES (@visitor, @business, 'VIEW_PROFILE');
SELECT activity_id, activity_type FROM user_activity WHERE user_id = @visitor;

SELECT 'TEST 19 - deleting a listing keeps the activity row (SET NULL)' AS test;
DELETE FROM user_activity WHERE business_id = (SELECT business_id FROM business WHERE name = 'Extended Zone Eatery');
DELETE FROM business WHERE name = 'Extended Zone Eatery';
SELECT COUNT(*) AS orphaned_but_kept FROM user_activity WHERE user_id = @visitor;

SELECT 'TEST 20 - EXPECT ERROR 1451: cannot delete a category in use' AS test;
DELETE FROM category WHERE category_id = @category;

-- ---------------------------------------------------------------------
SELECT '--- ALL TESTS RUN: rolling back so the database is unchanged ---' AS step;
ROLLBACK;

-- Post-conditions: the fixtures are gone again.
SELECT COUNT(*) AS leftover_test_users FROM app_user
WHERE email LIKE 'triggers.%@cbdventures.test';
SELECT COUNT(*) AS leftover_test_listings FROM business
WHERE name IN ('Zone Test Kitchen', 'Extended Zone Eatery', 'Far Away Bakery');
