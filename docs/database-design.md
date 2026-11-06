# Database design — CBDVentures (MySQL 8 production schema)

This document describes the physical database, the integrity rules it enforces, and the
reasoning behind the two designs that were merged into it: this project's normalised
schema and the schema published in the group's `CBDVentures` repository
(`cbdventures_db_final_checkpoint.sql` / `cbdventures_db_triggers_backup_all_six.sql`).

| Item | Value |
|---|---|
| Tables | **12** |
| Indexes (excluding primary keys) | **21** |
| Foreign keys | **15** |
| CHECK constraints | **11** |
| Triggers | **8** |
| Normal form | 3NF (every non-key attribute depends on the key, the whole key, and nothing but the key) |
| Files | `db/schema_mysql.sql`, `db/triggers_mysql.sql`, `db/tests/trigger_tests.sql` |
| Demo database | H2 file database (`./data/cbdventures`), same entities via JPA |
| Production profile | `spring.profiles.active=mysql` → `application-mysql.yml` |

---

## 1. Tables

| # | Table | Purpose | Key relationships |
|---|---|---|---|
| 1 | `app_user` | Students, visitors, business owners, administrators | Parent of `business`, `review`, `user_preference`, `user_activity` |
| 2 | `category` | The **five fixed** discovery categories | Parent of `business`, `amenity`, `preference_category` |
| 3 | `amenity` | Reusable facility tags (Wi-Fi, plugs, quiet, …) | Optional FK to `category` |
| 4 | `business` | A listing inside the pilot zone | FK `owner_id` → `app_user` (RESTRICT), FK `category_id` → `category` (RESTRICT) |
| 5 | `business_amenity` | Many-to-many junction (resolves the 1NF repeating group) | Composite PK (`business_id`, `amenity_id`) |
| 6 | `business_image` | Profile photos, exactly one primary | FK → `business` CASCADE |
| 7 | **`business_hours`** | **Opening hours, one row per weekday** | UNIQUE (`business_id`, `day_of_week`), FK → `business` CASCADE |
| 8 | `event` | Event listings attached to a venue | FK → `business` CASCADE |
| 9 | `review` | One review per user per business | UNIQUE (`user_id`, `business_id`), both FKs CASCADE |
| 10 | `user_preference` | Saved price/distance ceiling | UNIQUE `user_id`, FK CASCADE |
| 11 | `preference_category` | Preferred categories | Composite PK |
| 12 | **`user_activity`** | Browsing trail for personalisation | FK `user_id` CASCADE, FK `business_id` **SET NULL** |

Tables 7 and 12 are the two additions taken from the group's design; everything else was
already present in this project's schema.

---

## 2. Integrity rules

### Entity integrity
* Every table has a `BIGINT AUTO_INCREMENT` surrogate primary key.
* `business_amenity` and `preference_category` use composite primary keys because the
  relationship itself is the entity — a duplicate row would be meaningless data.

### Referential integrity
| Parent | Child | On delete | On update | Reason |
|---|---|---|---|---|
| `category` | `business` | **RESTRICT** | CASCADE | A category with listings must not vanish |
| `app_user` (owner) | `business` | **RESTRICT** | CASCADE | An owner with listings must be reassigned first |
| `business` | `business_image`, `business_hours`, `event`, `review`, `business_amenity` | **CASCADE** | CASCADE | These rows have no meaning without their listing |
| `app_user` | `review`, `user_preference`, `preference_category`, `user_activity` | **CASCADE** | CASCADE | Personal data goes with the account (POPIA) |
| `business` | `user_activity` | **SET NULL** | CASCADE | Aggregate interest survives a listing leaving the catalogue |
| `amenity` | `business_amenity` | CASCADE | CASCADE | Pure link rows |

### Domain integrity
* `CHECK (rating BETWEEN 1 AND 5)` — an integer star rating, never 0 or 6.
* `CHECK (latitude BETWEEN -90 AND 90)`, `CHECK (longitude BETWEEN -180 AND 180)`.
* `CHECK (day_of_week BETWEEN 1 AND 7)` — ISO weekday numbers.
* `CHECK` on `business_hours`: a closed day stores no times; an open day stores both, with
  `open_time < close_time`.
* `CHECK` on `business`: `open_time`/`close_time` are both null or both set.
* `CHECK` on `event`: `ticket_price >= 0` (0.00 means free).
* `ENUM` for every closed value set: `role`, `price_level`, `status`, `zone_radius`,
  `activity_type`.
* `UNIQUE` on `app_user.email`, `amenity.amenity_name`, `category.slug`,
  `business_hours (business_id, day_of_week)`, `review (user_id, business_id)`,
  `user_preference.user_id`.

### User-defined (business) integrity — enforced in the database by triggers
| Trigger | Fires on | Rule |
|---|---|---|
| `trg_business_zone_insert` | BEFORE INSERT `business` | Listing must be within 1 000 m (primary) or 5 000 m (extended) of Richfield |
| `trg_business_zone_update` | BEFORE UPDATE `business` | An update may not move a listing out of its zone |
| `trg_review_no_self_review` | BEFORE INSERT `review` | An owner may not review their own business |
| `trg_review_edit_window` | BEFORE UPDATE `review` | A review may be edited once every 24 hours, and never moved to another user or business |
| `trg_event_future_insert` | BEFORE INSERT `event` | A new event must be in the future |
| `trg_event_future_update` | BEFORE UPDATE `event` | An event cannot be moved into the past |
| `trg_hours_window_insert` | BEFORE INSERT `business_hours` | Closed ⇒ no times; open ⇒ both times; opening before closing |
| `trg_hours_window_update` | BEFORE UPDATE `business_hours` | Same rules on edit |

All triggers raise `SIGNAL SQLSTATE '45000'`, which clients report as **error 1644** with a
sentence that says what to fix.

**Why both triggers and Java validation?** The API already rejects bad input
(`ZoneService`, `BusinessService`, `ReviewService`). Triggers add a second, independent layer
so the rules also hold for a DBA in Workbench, a CSV import, or a future Android client.
This is defence in depth, not duplication for its own sake: the Java layer produces friendly
field-level messages, the database layer guarantees the invariant.

---

## 3. Why per-day hours instead of one window

The earlier design stored a single `open_time`/`close_time` plus a display string. That
cannot express the most common real pattern in the CBD — *open Monday to Saturday, closed on
Sunday* — so "is it open now?" was wrong every Sunday.

`business_hours` fixes this with one row per weekday:

```
business_hours(hours_id, business_id, day_of_week, is_closed, open_time, close_time)
UNIQUE (business_id, day_of_week)
```

* `day_of_week` is the ISO number (1 = Monday … 7 = Sunday), so `WEEKDAY(CURDATE()) + 1`
  answers "today" in one expression.
* `UNIQUE (business_id, day_of_week)` makes a duplicated weekday impossible.
* `BusinessMapper.openNow()` reads today's row first and falls back to the legacy
  `business.open_time`/`close_time` columns, which are kept and documented as a fallback for
  older API clients and single-window listings. Both paths are covered by tests.
* The owner form posts a seven-row week; `BusinessService.buildHours()` validates it before
  anything is written, and the entity replaces the week atomically.

## 4. What `user_activity` is for

Personalised suggestions previously used saved preferences and past reviews. That misses the
common case: a visitor browses three coffee spots in one afternoon and never reviews any of
them. `user_activity` records `VIEW_PROFILE`, `SEARCH` and `SAVE_BUSINESS`, and
`RecommendationService.affinityWeights()` adds them to the category weights:

| Signal | Weight | Rationale |
|---|---|---|
| Saved preference category | 3.0 | An explicit statement of intent |
| Review ≥ 4★ | 1.5 | Deliberate endorsement |
| Review ≤ 3★ | 0.5 | Still interest, but weaker |
| `SAVE_BUSINESS` | 0.75 | Chose to keep it |
| `VIEW_PROFILE` | 0.25 | A glance |
| `SEARCH` | 0.10 | Weakest signal |

Guests are never tracked, an owner viewing their own listing is not counted, and a repeat view
of the same listing within 30 minutes is ignored so refreshing a page does not inflate the
weight.

---

## 5. Verification

```bash
# Build the schema and the triggers
mysql -u root -p < db/schema_mysql.sql
mysql -u root -p cbdventures < db/triggers_mysql.sql      # prints the 8 installed triggers

# Prove every rule fires (20 checks, all rolled back at the end)
mysql -u root -p --force cbdventures < db/tests/trigger_tests.sql
```

`db/tests/trigger_tests.sql` covers, in order: a valid listing inside the zone, a listing
3 km away rejected (1644), a move out of the zone rejected (1644), the same listing accepted
in the extended zone, seven hours rows, a duplicated weekday rejected (1062), opening after
closing rejected (1644), a closed day with times rejected (1644), hours for a missing listing
rejected (1452), a first review accepted, a second review by the same person rejected (1062),
an owner reviewing their own business rejected (1644), a rating of 9 rejected (4025), an
edit inside 24 hours rejected (1644), a future event accepted, a past event rejected (1644),
moving an event into the past rejected (1644), an activity row recorded, activity surviving a
listing delete, and a category in use refusing to delete (1451). The script ends with
`ROLLBACK`, and the two post-condition counts confirm nothing was left behind.

On the application side, `./mvnw test` runs **73** tests, of which
`OpeningHoursTest` (14) covers the per-day hours entity, the service validation rules, the
legacy fallback, the card/profile mapping and the activity trail.

---

## 6. Credit

Three ideas in this schema come from the group's database design and are used here with
gratitude:

1. **Database triggers for the 1 km zone, future events, self-review and the 24-hour review
   window.** Their trigger bodies were the starting point; two were tightened for this
   project (see below).
2. **`business_hours` with one row per weekday**, replacing the flat single window.
3. **`user_activity`** as a personalisation signal, including `ON DELETE SET NULL` on the
   listing reference.

Two corrections were applied while adopting them:

* Their 24-hour trigger limited *repeat reviews* of the same business, which still allowed one
  review per person per day. The brief requires **one review per user per business**, so the
  rule moved to `UNIQUE (user_id, business_id)` and the 24-hour limit now guards edits only.
* Their `business_hours` table had no open-before-close check (acknowledged in their own
  `Data_Security_and_Integrity.docx`), and `business.owner_id` was nullable. Both are closed
  here with a CHECK/trigger and a `NOT NULL` owner.
