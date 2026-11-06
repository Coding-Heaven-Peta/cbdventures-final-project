-- CBD VENTURES | DATABASE TESTS | MySQL Workbench | 20 October 2026
-- Tested schema: cbdventures_db_clean
-- Run sections separately. Intentional error 1644 means a rejection test PASSES.
-- ALWAYS run ROLLBACK after each transaction-based test, including after an error.
-- Do not run these tests against production or the restored backup.
USE cbdventures_db_clean;

-- TEST 1: 12 base tables
SELECT COUNT(*) AS total_tables FROM information_schema.tables
WHERE table_schema = 'cbdventures_db_clean' AND table_type='BASE TABLE';

-- TEST 2: 39 baseline records (other six tables expected empty)
SELECT 'user' AS table_name, COUNT(*) AS total_records FROM `user`
UNION ALL SELECT 'category', COUNT(*) FROM category
UNION ALL SELECT 'business', COUNT(*) FROM business
UNION ALL SELECT 'business_hours', COUNT(*) FROM business_hours
UNION ALL SELECT 'amenity', COUNT(*) FROM amenity
UNION ALL SELECT 'business_amenity', COUNT(*) FROM business_amenity;

-- TEST 3: 6 installed triggers
SELECT TRIGGER_NAME, EVENT_MANIPULATION, EVENT_OBJECT_TABLE
FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA='cbdventures_db_clean';

-- TEST 4: INVALID BUSINESS INSERT (expected error 1644)
INSERT INTO business (name,address,latitude,longitude)
VALUES ('TEST Invalid Location','Cape Town Test Address',-33.9249,18.4241);
-- If unexpectedly accepted, STOP and remove only that test row after inspection.

-- TEST 5: PAST EVENT INSERT (expected error 1644)
INSERT INTO event (business_id,title,venue,event_date,start_time)
VALUES (1,'TEST Past Event','Pretoria Test Venue','2020-01-01','10:00:00');

-- TEST 6: SELF REVIEW (run each statement; expected INSERT error 1644)
START TRANSACTION;
UPDATE business SET owner_id=1 WHERE business_id=1;
INSERT INTO review (user_id,business_id,rating,comment)
VALUES (1,1,5,'TEST Self Review');
ROLLBACK; -- Run separately if Workbench stops at the error.
SELECT business_id,owner_id FROM business WHERE business_id=1;

-- TEST 7: 24-HOUR DUPLICATE REVIEW (run each statement in order)
-- PRECONDITION: no recent review by user 1 of business 1. Check first:
SELECT review_id,user_id,business_id,created_at FROM review
WHERE user_id=1 AND business_id=1;
-- If recent review exists, choose an eligible pair; do not delete genuine reviews.
START TRANSACTION;
INSERT INTO review (user_id,business_id,rating,comment)
VALUES (1,1,5,'First test review'); -- expected success (1 row)
INSERT INTO review (user_id,business_id,rating,comment)
VALUES (1,1,4,'Second test review'); -- expected error 1644
ROLLBACK; -- Run separately after the error.
SELECT review_id,user_id,business_id,comment FROM review;

-- TEST 8: INVALID BUSINESS LOCATION UPDATE (expected error 1644)
START TRANSACTION;
UPDATE business SET latitude=-33.9249,longitude=18.4241 WHERE business_id=1;
ROLLBACK; -- Run separately after the error.

-- TEST 9: PAST EVENT UPDATE (future test event, then expected error 1644)
START TRANSACTION;
INSERT INTO event (business_id,title,venue,event_date,start_time)
VALUES (1,'TEST Future Event','Pretoria Test Venue','2027-12-01','10:00:00');
SET @test_event_id=LAST_INSERT_ID();
UPDATE event SET event_date='2020-01-01' WHERE event_id=@test_event_id;
ROLLBACK; -- Run separately after the error.
SELECT event_id,title,event_date FROM event;

-- TEST 10: Backup export performed using Workbench Server > Data Export;
-- self-contained file; structure and data; Include Create Schema; Dump Triggers.
-- This GUI operation has no equivalent SQL statement in this test script.

-- TEST 11: Verify independently restored schema (read-only)
SELECT
 (SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema='cbdventures_db_restore_test' AND table_type='BASE TABLE') AS total_tables,
 (SELECT COUNT(*) FROM information_schema.triggers
  WHERE trigger_schema='cbdventures_db_restore_test') AS total_triggers;
SELECT
 (SELECT COUNT(*) FROM cbdventures_db_restore_test.`user`) +
 (SELECT COUNT(*) FROM cbdventures_db_restore_test.category) +
 (SELECT COUNT(*) FROM cbdventures_db_restore_test.business) +
 (SELECT COUNT(*) FROM cbdventures_db_restore_test.business_hours) +
 (SELECT COUNT(*) FROM cbdventures_db_restore_test.amenity) +
 (SELECT COUNT(*) FROM cbdventures_db_restore_test.business_amenity) AS total_records;
