-- =====================================================================
--  CBDVentures — MySQL triggers (server-side integrity, defence in depth)
--  IT Project Group 5 · Phase 3 (Implementation)
--
--  Why triggers as well as Java validation?
--  The API already rejects bad input (ZoneService, BusinessService,
--  ReviewService). Triggers add a second, independent layer so the same rules
--  hold for *any* writer: a DBA in Workbench, a CSV import, a future Android
--  client, or a rushed hotfix. This mirrors the group's design, and closes the
--  gaps in it (see the notes on TR-03 and TR-04).
--
--  Run after db/schema_mysql.sql:
--    mysql -u root -p cbdventures < db/triggers_mysql.sql
--
--  Errors are raised with SIGNAL SQLSTATE '45000', which the client surfaces
--  as error 1644 with the message text below. The tests in
--  db/tests/trigger_tests.sql assert on exactly those messages.
--
--  Inventory (8 triggers)
--    TR-01  trg_business_zone_insert     BEFORE INSERT  business
--    TR-02  trg_business_zone_update     BEFORE UPDATE  business
--    TR-03  trg_review_no_self_review    BEFORE INSERT  review
--    TR-04  trg_review_edit_window       BEFORE UPDATE  review
--    TR-05  trg_event_future_insert      BEFORE INSERT  event
--    TR-06  trg_event_future_update      BEFORE UPDATE  event
--    TR-07  trg_hours_window_insert      BEFORE INSERT  business_hours
--    TR-08  trg_hours_window_update      BEFORE UPDATE  business_hours
-- =====================================================================

USE cbdventures;

DROP TRIGGER IF EXISTS trg_business_zone_insert;
DROP TRIGGER IF EXISTS trg_business_zone_update;
DROP TRIGGER IF EXISTS trg_review_no_self_review;
DROP TRIGGER IF EXISTS trg_review_edit_window;
DROP TRIGGER IF EXISTS trg_event_future_insert;
DROP TRIGGER IF EXISTS trg_event_future_update;
DROP TRIGGER IF EXISTS trg_hours_window_insert;
DROP TRIGGER IF EXISTS trg_hours_window_update;

DELIMITER $$

-- ---------------------------------------------------------------------
-- TR-01 / TR-02  Every listing must sit inside the pilot zone
--
-- Anchor = Richfield campus, Pretoria CBD (-25.746350, 28.192550), matching
-- cbdventures.zone.anchor-latitude/longitude. Distance is the great-circle
-- distance in metres (MySQL 8 ST_Distance_Sphere works on POINT(longitude,
-- latitude) — note the x/y order, which is the usual source of bugs here).
--
-- A listing may be up to 1 000 m away when it is in the primary zone, or
-- 5 000 m away when the extended zone is enabled for it, which is exactly the
-- rule ZoneService.classify() implements in Java.
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_business_zone_insert
BEFORE INSERT ON business
FOR EACH ROW
BEGIN
    DECLARE metres DECIMAL(12, 2);
    DECLARE allowed INT;
    SET metres = ST_Distance_Sphere(
            POINT(NEW.longitude, NEW.latitude),
            POINT(28.192550, -25.746350));
    SET allowed = CASE NEW.zone_radius
                      WHEN 'PRIMARY_1KM' THEN 1000
                      WHEN 'EXTENDED_5KM' THEN 5000
                      ELSE NULL
                  END;

    IF allowed IS NULL OR metres > allowed THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'TR-01 Listing is outside the allowed pilot zone (1 km primary / 5 km extended) from Richfield.';
    END IF;
END$$

CREATE TRIGGER trg_business_zone_update
BEFORE UPDATE ON business
FOR EACH ROW
BEGIN
    DECLARE metres DECIMAL(12, 2);
    DECLARE allowed INT;
    SET metres = ST_Distance_Sphere(
            POINT(NEW.longitude, NEW.latitude),
            POINT(28.192550, -25.746350));
    SET allowed = CASE NEW.zone_radius
                      WHEN 'PRIMARY_1KM' THEN 1000
                      WHEN 'EXTENDED_5KM' THEN 5000
                      ELSE NULL
                  END;

    IF allowed IS NULL OR metres > allowed THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'TR-02 Listing update moved it outside the allowed pilot zone from Richfield.';
    END IF;
END$$

-- ---------------------------------------------------------------------
-- TR-03  An owner may not review their own business
--
-- The group's version compared NEW.user_id with business.owner_id, which is
-- correct, but they had no protection against a *changed* owner afterwards
-- and no unique key. Ours is paired with UNIQUE (user_id, business_id) in the
-- schema, so the "one review per user per business" rule is enforced by the
-- index and this trigger only has to police the owner relationship.
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_review_no_self_review
BEFORE INSERT ON review
FOR EACH ROW
BEGIN
    DECLARE listing_owner BIGINT;
    SELECT owner_id INTO listing_owner FROM business WHERE business_id = NEW.business_id;
    IF listing_owner = NEW.user_id THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'TR-03 A business owner cannot review their own business.';
    END IF;
END$$

-- ---------------------------------------------------------------------
-- TR-04  A review can only be edited once every 24 hours
--
-- The group's trigger was a 24-hour limit on *new* reviews of the same
-- business, which allowed the same person to leave a second review a day
-- later. The brief says one review per user per business, so the unique key
-- does that job; what remains is the anti-spam cooldown on edits, which is
-- what this trigger enforces (and what ReviewService returns as HTTP 429).
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_review_edit_window
BEFORE UPDATE ON review
FOR EACH ROW
BEGIN
    IF NEW.user_id <> OLD.user_id OR NEW.business_id <> OLD.business_id THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'TR-04 A review cannot be moved to another user or business.';
    END IF;
    IF NEW.rating <> OLD.rating
       OR COALESCE(NEW.comment, '') <> COALESCE(OLD.comment, '') THEN
        IF TIMESTAMPDIFF(HOUR, OLD.updated_at, NOW()) < 24 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'TR-04 You can update your review once every 24 hours.';
        END IF;
    END IF;
END$$

-- ---------------------------------------------------------------------
-- TR-05 / TR-06  Events must be in the future
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_event_future_insert
BEFORE INSERT ON event
FOR EACH ROW
BEGIN
    IF NEW.event_date_time IS NULL OR NEW.event_date_time <= NOW() THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'TR-05 A new event must have a date and time in the future.';
    END IF;
END$$

CREATE TRIGGER trg_event_future_update
BEFORE UPDATE ON event
FOR EACH ROW
BEGIN
    IF NEW.event_date_time IS NULL OR NEW.event_date_time <= NOW() THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'TR-06 An event update cannot move the event into the past.';
    END IF;
END$$

-- ---------------------------------------------------------------------
-- TR-07 / TR-08  Opening-hours sanity
--
-- chk_hours_window already covers this inside a single row, and is the first
-- line of defence; the triggers restate the rule in plain language so that the
-- error a developer sees explains what to fix. A closed day carries no times;
-- an open day carries both, with opening strictly before closing.
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_hours_window_insert
BEFORE INSERT ON business_hours
FOR EACH ROW
BEGIN
    IF NEW.is_closed = TRUE AND (NEW.open_time IS NOT NULL OR NEW.close_time IS NOT NULL) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'TR-07 A closed day cannot have opening or closing times.';
    END IF;
    IF NEW.is_closed = FALSE AND (NEW.open_time IS NULL OR NEW.close_time IS NULL) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'TR-07 An open day needs both an opening and a closing time.';
    END IF;
    IF NEW.is_closed = FALSE AND NEW.open_time >= NEW.close_time THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'TR-07 The opening time must be earlier than the closing time.';
    END IF;
END$$

CREATE TRIGGER trg_hours_window_update
BEFORE UPDATE ON business_hours
FOR EACH ROW
BEGIN
    IF NEW.is_closed = TRUE AND (NEW.open_time IS NOT NULL OR NEW.close_time IS NOT NULL) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'TR-08 A closed day cannot have opening or closing times.';
    END IF;
    IF NEW.is_closed = FALSE AND (NEW.open_time IS NULL OR NEW.close_time IS NULL) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'TR-08 An open day needs both an opening and a closing time.';
    END IF;
    IF NEW.is_closed = FALSE AND NEW.open_time >= NEW.close_time THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'TR-08 The opening time must be earlier than the closing time.';
    END IF;
END$$

DELIMITER ;

-- =====================================================================
--  Verification
-- =====================================================================
-- Expect 8 rows.
SELECT TRIGGER_NAME, EVENT_MANIPULATION, EVENT_OBJECT_TABLE, ACTION_TIMING
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA = 'cbdventures'
ORDER BY EVENT_OBJECT_TABLE, ACTION_TIMING;

-- Behavioural checks live in db/tests/trigger_tests.sql so they can be run
-- (and re-run) independently of creating the triggers.
