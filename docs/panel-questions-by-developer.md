# Judging panel — likely questions per developer

**CBDVentures · IT Project · Group 5 · Supervisor: Ma'am Ntokozo**

Prepared from the code as submitted (light build) and the dark-mode build. Every question below can be
answered from files actually in the ZIP — the "evidence to have open" line under each role tells you
what to point at if the panel asks you to prove it.

> **How to use this:** the roles follow the split already written into `docs/evaluation-compliance.md`
> (Individual evaluation → "Knowledge of the system"). Fill your names into the headings, and each person
> memorises their own 5 core questions cold. The **cross-cutting** section at the end is fair game for
> *any* member — that is where panels separate people who built a module from people who only demoed one.

---

## Ground rules the panel will assume

| Assumption | What it means for you |
|---|---|
| Every member can run the system | Know `./mvnw spring-boot:run` → `http://localhost:8080`, and that the demo DB is H2 (no MySQL install needed). |
| Every member knows the group's SDLC | Waterfall, Phase 1 (analysis) → Phase 2 (design) → Phase 3 (implementation). Be able to name one artefact per phase. |
| Questions are answered in the first person | "I built…", "I tested…" — not "we think someone did…". If you didn't build it, say what you integrated against. |
| You can be asked to *do* it live | Rehearse the click path; don't hunt for buttons under pressure. |

---

# Role 1 — Discovery & search

*`BusinessService`, `BusinessQuery`, `BusinessSpecifications`, `ZoneService` · `GET /api/businesses`*

**Evidence to have open:** Explore screen with a filter applied, plus `src/main/java/.../service/BusinessQuery.java`.

1. **Walk me through what happens when I type "kota" and tap search — from the keystroke to the card on screen.**
   *Cover:* debounce in the view → `GET /api/businesses?q=kota` → `BusinessQuery` builds a `Specification` → matches name, description, address, category name and amenity names → results mapped to `BusinessCard` with rating, distance and walking minutes → rendered as a list.
2. **Which fields does your search actually look in, and why those?**
   *Cover:* name, description, address, category, amenity — because a student searches by *thing* ("kota"), *place* ("Schubart Street") or *feature* ("backup power"), not by database column.
3. **How do the filters combine — AND or OR?**
   *Cover:* different filters are ANDed (category **and** price **and** distance); repeated values inside one filter (two amenities) are each required. Get this wrong and the panel will notice when two amenities return more results than one.
4. **Why is the price filter 1, 2 and 3 and not "R", "RR", "RRR"?**
   *Strong answer:* the API accepts numeric levels `1..3` and maps them to `PriceLevel` enums (`1=R`, `2=RR`, `3=RRR`). The UI shows the symbols but sends the number. (This was a real bug we hit: sending the level *names* silently matched nothing.)
5. **What is the 1 km zone, and where is that number defined?**
   *Cover:* anchor = Richfield Pretoria Campus, `-25.746350 / 28.192550`, in `application.yml` (`primary-radius-km: 1.0`, `extended-radius-km: 5.0`, extended disabled). Products must be inside it to be discoverable — that is the business model, not a technical limit.
6. **Probe: how do you compute distance and walking time — do you call Google Maps?**
   *Cover:* no external call. Haversine in `GeoUtils`, then walking minutes = distance / ~80 m per minute, rounded, minimum 1. Deliberate: works offline and costs nothing.
7. **Probe: 26 listings fit on one screen. What breaks at 10 000, and what would you change first?**
   *Cover:* paging already exists (`page`, `size`, max 100) — but filters run in memory for some paths; first change is pushing zone filtering and sorting into SQL with a spatial index, then a search index (or MySQL full-text) for `q`.
8. **Probe: what happens if the user's GPS says they're in Cape Town?**
   *Cover:* `lat`/`lng` override the reference point so distances stay honest, but the *zone rule* still filters listings — you can't browse Pretoria listings as "walking distance" from Cape Town.

---

# Role 2 — Business profiles & reviews

*`BusinessService.detail`, `ReviewService`, `BusinessMapper`, `business_hours` · `GET /api/businesses/{id}`, `POST /api/businesses/{id}/reviews`*

**Evidence to have open:** Kota Kingdom profile (hours table + rating distribution), and the review modal.

1. **Talk me through the profile page — what data is behind each block?**
   *Cover:* `business` + `business_image` + `business_amenity`/`amenity` + `business_hours` (7 rows) + `review` + `event`; rating is an aggregate over `review`; map pin uses the stored lat/lng against the 1 km ring.
2. **A shop is open 07:00–17:00 Monday to Friday and closed on Sunday. How is "Open now" decided?**
   *Cover:* `business_hours` holds one row per weekday with open/close times; `openTime`/`closeTime` on `business` are a summary. `OpeningHoursTest` (14 tests) covers closed days, overnight windows and boundary minutes like exactly 17:00.
3. **What stops me leaving ten 5-star reviews and faking the rating?**
   *Cover:* one review per user per business, enforced by a unique constraint **and** a database trigger; a second submission edits the existing one. Owners and admins can't review their own listing. Edits are rate-limited to once per 24 hours.
4. **Does the average rating update immediately? Why?**
   *Cover:* yes — the review is committed, then the profile re-fetches, so the rating and the distribution bars recalculate server-side. No cached number.
5. **Probe: what's the difference between `canReview` and `myReview` — and which bug did you hit?**
   *Strong answer:* `canReview` is false when you *already* have a review, so a visitor with an existing review had no way to edit it. We fixed the button to check `myReview` first, then `canReview`. (A genuine bug found in testing — say so; ownership of a bug is a plus.)
6. **Probe: how is the "Verified" badge on a review earned?**
   *Cover:* the reviewer is an account that passed e-mail verification; it is a trust signal, not a purchase.
7. **Probe: a review contains HTML or a script tag. What happens?**
   *Cover:* input validated server-side, and every view escapes user text (`escapeHtml`) before it reaches the DOM — so no stored XSS.

---

# Role 3 — Owner portal

*`OwnerController` · `/api/owner/**`*

**Evidence to have open:** owner dashboard signed in as `thabo@kotakingdom.co.za` / `Password123!`.

1. **Sign in as an owner and show me the whole journey of adding a listing.**
   *Cover:* dashboard lists only *my* listings (`GET /api/owner/businesses`) → "Add listing" form (name, category, address, lat/lng, price, seven-day hours grid, amenities, phone) → live zone feedback showing the measured distance → `POST /api/owner/businesses` → status **PENDING**, not visible in public search.
2. **Where does that new listing go, and who can see it?**
   *Cover:* into `business` with `status = PENDING`. The owner sees it; the public cannot — `BusinessSpecifications` only returns `ACTIVE` for public browsing. The admin queue picks it up.
3. **How does editing differ from creating?**
   *Cover:* `PUT /api/owner/businesses/{id}` is owner-scoped (record-level check, not just role) and a re-edited live listing goes back for review, so an approved shop can't switch to something else unmoderated. `DELETE` is a soft dependency-safe removal.
4. **Show me events. Why do events hang off a business instead of standing alone?**
   *Cover:* an event must have a physical venue — that is the product's whole premise ("what's on within walking distance"). `event` has an FK to `business`; delete the venue and the events go with it.
5. **Probe: what stops me editing someone else's listing if I change the ID in the request?**
   *Cover:* every owner endpoint resolves the authenticated user and filters by `owner_id`, so a foreign ID returns 403/404 rather than data. Role alone is not enough — that's the record-level authorisation point on the rubric.
6. **Probe: I uploaded no photo and no phone number. Does the system care?**
   *Cover:* it publishes (they're optional) but it scores lower in recommendations — completeness is literally part of the ranking formula (photo +8, contact +5). Good answer ties Role 3 to Role 7.

---

# Role 4 — Admin verification & security

*`AdminController`, `AdminService`, `AuthService`, `SecurityConfig` · `/api/admin/verifications`, `/api/auth/**`*

**Evidence to have open:** admin queue as `admin@cbdventures.co.za` / `Admin123!`, and a guest hitting a protected endpoint (401).

1. **Walk me through approving a pending listing.**
   *Cover:* queue lists PENDING (+ rejected with the prior note) → admin reviews details, address and measured distance from the anchor → approve sets `ACTIVE` and the listing appears in Explore; reject requires a note the owner sees and can fix and resubmit.
2. **Explain your login security end to end.**
   *Cover:* password hashed with BCrypt (never stored or logged in clear), `POST /api/auth/login` returns a **JWT (HS256)** bearer token, the client sends `Authorization: Bearer …`, a filter resolves the user, three roles VISITOR / OWNER / ADMIN.
3. **What's the difference between 401 and 403 in your system — show me.**
   *Cover:* 401 = you are not signed in (guest hitting a protected endpoint); 403 = you are signed in but not allowed (owner opening `#/admin`). Distinct status codes, distinct screens.
4. **Could I just edit the JWT in the browser and become admin?**
   *Cover:* no — the signature is verified with the server secret; a tampered token is rejected (there is a test for exactly this: `tamperedTokenIsRejected`). The role inside the token is trusted only after signature verification.
5. **What personal information do you store, and why that little?**
   *Cover:* full name + e-mail + hashed password, nothing else. POPIA data minimisation: no ID numbers, no addresses for visitors. This is defended as a design decision, not laziness.
6. **Probe: the dark-mode build adds e-mail verification. How does it work and what stops brute-forcing the code?**
   *Cover:* 6-digit code, 10-minute TTL, 5 attempts then invalidated, resend throttled to 30 s (429). Registration no longer returns a token; you must verify first, and unverified sign-in gets 403. Codes are held in memory because a demo needs no SMTP; the production change is to persist them and send via mail.
7. **Probe: what is `@PreAuthorize` / where exactly do you enforce authorisation — controller or service?**
   *Cover:* role checks at the route, record-level ownership inside the service where the data is loaded — because "is this mine?" needs the row.
8. **Probe: name one security thing you know is still weak.**
   *Strong answer:* the JWT lives in `localStorage`, so XSS would expose it; httpOnly cookies + CSRF tokens are the fix. Also no rate limiting on login attempts. Admitting this with a fix shows judgement.

---

# Role 5 — Reports

*`ReportService`, `AdminReportingController` · `/api/admin/reports`, `/api/admin/reports/{key}`, `…/csv`*

**Evidence to have open:** `#/reports`, each of the five tabs, one CSV downloaded and opened.

1. **Name your five reports and the business question each one answers.**
   *Cover:* **Listings by category** (where is supply concentrated, and how much is stuck in review) · **Verification audit** (how fast are submissions cleared, rejection rate) · **Review activity** (is the rating base real and recent) · **Owner performance** (who deserves promotion / needs help) · **Events schedule** (what's coming, is the calendar alive).
2. **Where does the data come from — views, stored procedures, or code?**
   *Cover:* aggregation queries in `ReportService` over the normal tables; no materialised views, so a report is never stale and the backup/restore can't desynchronise it.
3. **Why CSV, and what's actually in the file?**
   *Cover:* opens in Excel for the marker/business owner; header row + one row per entity, generated server-side with correct escaping and a filename dated today.
4. **Your numbers: 26 live, 2 pending, 1 rejected — how do I know the report is right?**
   *Cover:* the same counts are asserted in `ReportingTest` (12 tests) and match the admin dashboard tiles; the totals row is computed, not hard-coded.
5. **Probe: a report takes 4 seconds with 10 000 listings. What do you do?**
   *Cover:* index the filter/group columns (already 21 indexes for the hot paths), push aggregation into SQL with `GROUP BY` instead of loading entities, cache per-report for a few minutes, and page the CSV export.
6. **Probe: who is allowed to see reports, and what happens if an owner asks for one?**
   *Cover:* ADMIN only, 403 for everyone else. An owner has their own performance view in the portal instead — need-to-know.

---

# Role 6 — System backup & restore

*`BackupService`, `BackupDtos`, `backup-and-restore.md` · `/api/admin/backup`, `/api/admin/backup/restore`*

**Evidence to have open:** `#/backup`, the downloaded `.json`, then the restore result.

1. **What exactly is in the backup file?**
   *Cover:* every table — categories, amenities, users (with hashed passwords, never plain), businesses, images, amenities join, hours, events, reviews, preferences, activity — one JSON document with counts.
2. **Walk me through a restore. What could go wrong and how did you handle it?**
   *Cover:* it runs in a **single transaction**; child tables are cleared and re-inserted in foreign-key-safe order (children before parents on delete, parents before children on insert); any failure rolls the whole thing back so you never get a half-restored database. This is exactly the ordering bug we had to fix in `schema_mysql.sql`.
3. **If restore is destructive, what stops an accidental wipe in production?**
   *Cover:* admin-only, explicit confirmation, and it warns that current data will be replaced. The production policy is `mysqldump` + scheduled snapshots (`docs/backup-and-restore.md`), with the JSON route for the demo/portability case.
4. **Prove the restore worked.**
   *Cover:* compare the counts before and after — 29 listings, 45 reviews, 12 events, 203 hours rows, 17 accounts — and the app still browses.
5. **Probe: what's the difference between your JSON backup and `mysqldump`?**
   *Cover:* JSON is portable and human-readable and can be re-imported through the API into any database; `mysqldump` captures the real MySQL schema, indexes, triggers and types exactly. Both are documented; neither replaces off-site storage.
6. **Probe: is a backup with plaintext everything a POPIA risk?**
   *Cover:* passwords stay hashed; the file still contains personal data (names, e-mails), so it's treated as sensitive — access-restricted, not e-mailed around, and noted in the docs.

---

# Role 7 — Recommendations & offline / frontend shell

*`Scoring`, `RecommendationService`, `ActivityService`, `ui.js`, offline cache · `/api/discover/home`, `/api/recommendations`*

**Evidence to have open:** home screen with "Recommended for you", then DevTools → Network → Offline.

1. **How does the app decide what I should see first?**
   *Cover:* a weighted score — **quality 40** (rating/5), **popularity 15** (review count, capped at 20 so a new place isn't buried), **proximity 25** (exponential decay over ~0.8 km), **completeness 13** (photo + contact + hours), then a bounded **personalisation bonus** from the categories you actually browsed (`user_activity` → affinity 0..1). `ScoringTest` (6 tests) pins the maths.
2. **Why cap popularity at 20 reviews?**
   *Cover:* otherwise the same three shops own the home screen forever and new listings can never surface — a fairness decision, and a good answer to "what would go wrong if you didn't".
3. **What is `user_activity` and don't you risk making the app worse for a brand-new user?**
   *Cover:* it records views/reviews/saves; with no history the personalisation term is zero, so they get the honest base ranking. Cold start degrades to popularity + proximity rather than breaking.
4. **Show me the app working with no internet.**
   *Cover:* cached listing payloads in `localStorage` serve the last feed; an offline banner appears; actions that need the server say so clearly instead of failing silently.
5. **Probe: what's your front-end architecture — one page or many?**
   *Cover:* single `index.html` shell, hash router in `app.js`, one module per screen in `js/views/`, shared API client and UI helpers — no framework, no build step, so it runs from the Spring Boot JAR with nothing to install.
6. **Probe: accessibility — what did you actually do?**
   *Cover:* skip link, ARIA labels and roles on dialogs/tabs, keyboard-focusable modals with focus restore, 4.5:1 contrast in both themes, respects reduced motion, works at 360 px.
7. **Probe: the dark-mode build — what changed in the backend for it?**
   *Cover:* e-mail verification and `GET /api/businesses/map` (real pins with coordinates/distance). Same domain, same schema, same tests — a new interface layer, not a fork. That answer protects the whole group if the panel treats the two versions as separate apps.

---

# Role 8 (if you have eight members) — Database, triggers & QA

*`db/schema_mysql.sql`, `db/triggers_mysql.sql`, `db/tests/trigger_tests.sql`, the test suites*

1. **Draw your schema from memory — entities and relationships.** *(12 tables: 3NF, 15 FKs, 21 indexes)*
2. **Why both application validation and database triggers? Isn't that duplication?**
   *Cover:* the API is one door; SQL is another. Triggers mean a rule can't be bypassed by an admin script or a future integration. 11 CHECK constraints + 8 triggers, with 20 checks in `trigger_tests.sql` proving the database refuses what the API refuses.
3. **Show me two things the database refuses that a form might not.**
   *Cover:* a listing 3 km from the anchor; an owner reviewing their own business; a second review by the same user; a closing time before opening time (rejected with the day named).
4. **74 tests in the light build, 76 in the dark — what do they actually cover?**
   *Cover:* `ApiHttpTest` (HTTP contracts, 401/403, tampered token, malformed id → 400), `DiscoveryIntegrationTest` (search/filter/zone), `OpeningHoursTest` (14), `ReportingTest` (12), `GeoUtilsTest` (7), `ZoneServiceTest` (6), `ScoringTest` (6).
5. **Probe: what's the most valuable test you wrote, and what's still untested?**
   *Strong answer:* the DB-rule and authorisation tests earn their keep because they catch silent regressions in security. Untested honestly: real-device/browser matrix and load.
6. **Probe: name the worst bug you personally fixed.** *(See the cross-cutting table — the host-ID and the 500-on-bad-ID stories are the best ones.)*

---

# Cross-cutting questions — anyone can be asked

| Question | What a strong answer contains |
|---|---|
| **Explain the SDLC phases you followed and name one artefact from each.** | Waterfall: Phase 1 analysis (requirements, FR-01…FR-04, problem statement) → Phase 2 design (ERD/`database-design.md`, wireframe, prototype PDF) → Phase 3 implementation (Spring Boot app, tests, this build). |
| **Why Spring Boot 4 / Java 25 — isn't that risky?** | Latest LTS-style stack as instructed, one runnable JAR, embedded Tomcat, no separate server to install for the marker; risk handled by pinning versions in `pom.xml` and the Maven wrapper so it builds identically anywhere. |
| **Where is your business logic — controller or service?** | Services: `BusinessService`, `ReviewService`, `AuthService`. Controllers only translate HTTP; DTOs validate input; `GlobalExceptionHandler` turns every failure into one friendly JSON shape. |
| **What happens if the database is down mid-demo?** | Requests fail with a clean 500 JSON, the offline banner appears, cached content stays browsable. No stack trace reaches the user. |
| **Show me a bug you found and how you found it.** | Best real stories: (1) **toasts and dialogs never appeared** — `index.html` declared `#toast-host`/`#modal-host` while `ui.js` wrote to `#toast-stack`/`#modal-root`, so every modal silently vanished; fixed the host IDs and made the router close modals on navigation. (2) **`/api/businesses/not-a-number` returned 500** — a path-variable type mismatch hit the catch-all; added a handler returning 400 with a test in **both** builds. (3) **Events screen crashed** with `list.querySelectorAll is not a function` — a local variable shadowed the results node. |
| **What would you do with another month?** | Real maps instead of the schematic zone map; SMTP-backed verification; image uploads with storage; a PWA service worker instead of manual caching; load testing; MySQL in production rather than H2. |
| **Two versions — "light" and "dark" — why?** | Same application, same API, same tests, same schema; dark mode is a second interface layer built from the five supplied screens, plus e-mail verification and a map endpoint. Evidence: both suites green. |
| **Who did what, and what did you personally not touch?** | Honest boundaries. Panels respect "I didn't build reports, but I integrated against `/api/admin/reports`" far more than false claims. |
| **If I gave you this app to maintain tomorrow, where do you start?** | Read `README.md` → `docs/api.md` → run the suite → `db/schema_mysql.sql`. The docs exist so a stranger can pick it up — that is the honest answer. |

---

## Numbers to know without looking

| Fact | Value |
|---|---|
| Stack | Spring Boot **4.1.2**, Java **25**, Maven wrapper, H2 (demo) / MySQL (production profile) |
| Database | **12 tables**, 3NF, 15 FKs, 21 indexes, 11 CHECK constraints, **8 triggers**, 20 rule checks |
| Tests | Light **74/74**, dark **76/76** green |
| Seed | 5 categories, 14 amenities, **29 listings** (26 live / 2 pending / 1 rejected), 203 hours rows, 45 reviews, 12 events, 17 accounts |
| Zone | Anchor Richfield Pretoria Campus, **1 km** primary, 5 km extended (disabled) |
| Modules | 7: discovery & search, profiles & reviews, owner portal, admin verification, reports, backup, recommendations |
| Demo logins | admin `admin@cbdventures.co.za` / `Admin123!` · owner `thabo@kotakingdom.co.za` / `Password123!` · visitor `keziah@student.richfield.ac.za` / `Password123!` |

---

## Two minutes before you walk in

1. Your own 5 core questions, out loud once — no notes.
2. Know your **one bug** and your **one trade-off** by heart.
3. Know the answer to "which part did you *not* build".
4. Have your screen already open on your module, seeded, signed in at the right role.
