# Evaluation sheet compliance — CBDVentures Group 5

Mapping of every line on the Richfield **Group Project Evaluation Sheet** (80 group marks)
to evidence in this project, plus what to demonstrate on the day.

| # | Standard | Marks | Where it is satisfied | How to demonstrate |
|---|---|---|---|---|
| 1 | **Running System** | 10 | Spring Boot 4.1.2 / Java 25 single-port application; `./mvnw spring-boot:run` → http://localhost:8080 | Start the server, open the home page, click through Discover → Explore → profile → Events. Nothing else to install (H2 demo database) |
| 2 | **Validation** | 10 | Bean Validation on every DTO, business rules in the services, **plus 8 MySQL triggers** so the same rules hold for direct SQL, and friendly JSON errors from `GlobalExceptionHandler` | Register with a bad email and a short password; submit a listing outside the 5 km boundary (shows the measured distance); set a closing time earlier than the opening time on a weekday row (rejected with the day named); leave two reviews (the second edits); try a rating of 9 |
| 3 | **Database** | 10 | **12 tables**, 3NF, 15 foreign keys, 21 indexes, 11 CHECK constraints, 8 triggers — `db/schema_mysql.sql` + `db/triggers_mysql.sql`; JPA entities mirror the schema; H2 demo + MySQL profile; 20 database rule tests in `db/tests/trigger_tests.sql` | Show `db/database-design.md`; run `mysql --force < db/tests/trigger_tests.sql` and point at the expected 1644 errors (a listing 3 km away is refused, an owner cannot review their own shop); open Sunday on a profile to show *Closed today*; H2 console at `/h2-console/` |
| 4 | **Form Links** | 10 | Every form is wired to the REST API: register/sign-in, listing create/edit, event create, review form, filter/sort bar, admin approve/reject, restore upload | Owner: create a listing → it appears as *Pending review*; admin approves it → it appears in Explore; leave a review on it |
| 5 | **System Backup** | 10 | `#/backup` — download every table as one JSON file, restore it again (single transaction); `mysqldump` and H2 file-copy instructions in `docs/backup-and-restore.md` | Download the backup, show the file size/contents, restore it, show the same counts |
| 6 | **Reports (3–5)** | 10 | **Five** reports at `#/reports`: Listings by category, Verification audit, Review activity, Owner performance, Events schedule — each with headline metrics and a CSV download | Click each of the five tabs, then *Download CSV* and open it in Excel |
| 7 | **Login and Security** | 10 | JWT (HS256) bearer auth, BCrypt password hashing, three roles (VISITOR / OWNER / ADMIN), route + record-level authorisation, 401 vs 403 distinction, POPIA-minimal data | Sign in as owner (own listings only), try `#/admin` as owner (denied), sign in as admin, show a guest gets 401 |
| 8 | **User Friendliness** | 5 | Responsive 360 px → desktop, mobile tab bar, plain-English messages, keyboard-accessible dialogs, skip link, ARIA labels, offline banner + cached listings | Resize the browser / open on a phone; go offline and show cached listings |
| 9 | **Modules (at least 2)** | 5 | **Seven** functional modules: Discovery & Search, Business Profiles & Reviews, Owner Portal, Admin Verification, Reports, System Backup, Personalised Recommendations | Walk through owner portal → admin verification → reports as separate modules |

**Group total available: 80.**

---

## Individual evaluation (20 marks each)

| Criterion | Preparation |
|---|---|
| **Knowledge of the system** | Each member should be able to explain their module end to end: the requirement (Phase 1–3), the table(s) behind it, the API endpoint, and the screen. Suggested split: (1) discovery & search, (2) profiles & reviews, (3) owner portal, (4) admin verification, (5) reports, (6) backup, (7) recommendations & offline |
| **Dressing** | Smart casual / formal, consistent across the group |
| **Confidence** | Rehearse the click path once so no one hunts for a button; keep the demo data seeded |
| **Time management** | Suggested 10-minute flow: 1 min intro & problem statement · 2 min student journey · 2 min owner journey · 2 min admin verification + reports · 2 min backup/restore · 1 min questions |

---

## Suggested demo script (10 minutes)

1. **Home** — hero search, five category tiles, personalised suggestions, stats strip.
2. **Explore** — filter by category + price + 500 m radius, sort by distance; point out the walking times.
3. **Profile** — Kota Kingdom: images, amenities, the **per-day opening hours table with today in bold and Sunday closed**, map pin with the 1 km ring, *Get directions*, review distribution.
4. **Review** — leave a 5★ review; the average recalculates immediately (one review per user rule).
5. **Owner portal** — sign in as `thabo@kotakingdom.co.za`; show the 3 listings, add one (fill the seven-day hours grid, mark Sunday *Closed*), note it becomes *Pending review*.
6. **Admin** — sign in as `admin@cbdventures.co.za`; approve the new listing; it appears in Explore.
7. **Reports** — open the five reports; download one CSV.
8. **Backup** — download the JSON backup, show the counts, then restore it.
9. **Security** — point out 401 for guests, 403 for a visitor, owner-only listing management, BCrypt hashes.
10. **Database rules** — run the trigger test script and show the database refusing a listing 3 km away, a second review by the same person and an owner reviewing their own shop.

---

## Evidence pack (what to hand in)

| Item | File |
|---|---|
| Source code | `cbdventures.zip` (this folder) |
| How to run | `README.md` §2 |
| API reference | `docs/api.md` |
| Requirements traceability | `docs/requirements-traceability.md` |
| Database schema | `db/schema_mysql.sql` · **`db/database-design.md`** · `db/triggers_mysql.sql` |
| Backup runbook | `docs/backup-and-restore.md` |
| Test evidence | `./mvnw test` → **73 passing tests**; `db/tests/trigger_tests.sql` → **20 database rule checks** |
| Screenshots | `screenshots/` inside the ZIP |

> The database now also carries the group's three best ideas (triggers, `business_hours`,
> `user_activity`); see `docs/database-design.md` §6 for exactly what was adopted and what was
> corrected while adopting it.
>
> The prototype PDF supplied with the brief is 0 bytes, so the interface follows the
> screens documented in the requirement documents (home search + category tiles, results
> with filter/sort bar, profile with map and reviews, review form with 1–5 stars, admin
> verification, owner listing management).
