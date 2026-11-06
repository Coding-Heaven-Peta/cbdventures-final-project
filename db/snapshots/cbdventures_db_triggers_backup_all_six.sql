-- CBDVentures trigger backup
-- Database: cbdventures_db
-- Contains the six trigger definitions currently reviewed in SQLPro.
--
-- IMPORTANT:
-- This file is a backup reference, not a script to run against the live
-- database while these triggers already exist. Running CREATE TRIGGER again
-- will fail with a duplicate-trigger-name error unless the existing triggers
-- are deliberately dropped first.
--
-- For restore via the MySQL command-line client, DELIMITER is a client-side
-- directive used to submit each trigger body as a single statement.

DELIMITER $$

CREATE TRIGGER trg_business_within_1km_insert
BEFORE INSERT ON business
FOR EACH ROW
BEGIN
    IF ST_Distance_Sphere(
        POINT(NEW.longitude, NEW.latitude),
        POINT(28.192483, -25.746400)
    ) > 1000 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Business location must be within 1 km of Richfield Pretoria Campus.';
    END IF;
END$$

CREATE TRIGGER trg_business_within_1km_update
BEFORE UPDATE ON business
FOR EACH ROW
BEGIN
    IF ST_Distance_Sphere(
        POINT(NEW.longitude, NEW.latitude),
        POINT(28.192483, -25.746400)
    ) > 1000 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Business location must be within 1 km of Richfield Pretoria Campus.';
    END IF;
END$$

CREATE TRIGGER trg_event_future_insert
BEFORE INSERT ON event
FOR EACH ROW
BEGIN
    IF TIMESTAMP(NEW.event_date, NEW.start_time) <= CURRENT_TIMESTAMP THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Event start date and time must be in the future.';
    END IF;
END$$

CREATE TRIGGER trg_event_future_update
BEFORE UPDATE ON event
FOR EACH ROW
BEGIN
    IF NEW.status = 'UPCOMING'
       AND TIMESTAMP(NEW.event_date, NEW.start_time) <= CURRENT_TIMESTAMP THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'An upcoming event must have a future start date and time.';
    END IF;
END$$

CREATE TRIGGER trg_review_no_self_review
BEFORE INSERT ON review
FOR EACH ROW
BEGIN
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
END$$

CREATE TRIGGER trg_review_24h_limit
BEFORE INSERT ON review
FOR EACH ROW
BEGIN
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
END$$

DELIMITER ;
