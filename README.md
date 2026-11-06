# CBDVentures — Pretoria CBD in Your Pocket

Hyper-local discovery platform for students, locals and visitors inside a **1 km radius of
Richfield Graduate Institute of Technology**, Pretoria Central.

`IT Project — Group 5` · Waterfall SDLC · **Phase 3 (Implementation) deliverable**

> **This folder is the DARK-MODE build (v2).** Same application, same REST API, same database
> schema and the same test suite as the original light build — only the interface layer differs
> (mobile-first dark theme built from the five supplied reference screens). See `DARK-MODE.md`
> for the screen-by-screen mapping, what was added to the backend, and a 10-minute demo script.
> The light build lives in the sibling folder `cbdventures/`.

---

## 1. What this repository contains

| Layer | Technology | Location |
|---|---|---|
| Backend REST API | **Java 25 · Spring Boot 4.1.2** (Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation) | `src/main/java` |
| Database (demo) | H2 in MySQL compatibility mode, file-backed in `./data` | `src/main/resources/application.yml` |
| Database (production) | **MySQL 8** schema, DDL + integrity constraints + verification queries | `db/schema_mysql.sql` |
| Web front end | HTML5 / CSS3 / vanilla JavaScript SPA (no framework, no build step) | `src/main/resources/static` |
| Automated tests | JUnit 5 — unit, integration and HTTP contract tests (**76 tests**) | `src/test/java` |
| Documentation | API reference and requirement traceability | `docs/` |

The web client is served by the same Spring Boot process, so the whole application runs on a
single port with a single command.

---

## 2. Run it

### Option A — demo mode (zero setup, recommended for marking)

```bash
cd cbdventures-dark
./mvnw spring-boot:run
```

Then open **http://localhost:8080** — the app opens on the welcome screen; choose
**Sign In** or **Browse without an account**.

> **Only one build at a time.** Both versions persist to the same file name
> (`./data/cbdventures.mv.db`), and H2 locks it — so do not run the light and dark builds
> simultaneously in the same environment. To show both side by side, give the second one its
> own port **and** database:
>
> ```bash
> PORT=8081 ./mvnw spring-boot:run \
>   -Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:file:./data/dark
> ```
>
> `PORT` is read by `application.yml` (`server.port: ${PORT:8080}`).

* Embedded H2 database (MySQL compatibility mode) persisted to `./data/cbdventures.mv.db`.
* On the first start the pilot zone is seeded with 5 categories, 14 amenities, 8 business
  owner accounts, **26 verified listings**, 45 reviews and 12 upcoming events — plus
  **3 submissions in the verification queue** (2 pending, 1 rejected) so the administrator
  flow can be demonstrated immediately.
* Restarting keeps your data; deleting the `data/` folder re-seeds from scratch.

### Option B — production profile (MySQL 8)

```bash
mysql -u root -p < db/schema_mysql.sql          # creates database + tables + reference data
export DB_USER=cbdventures DB_PASSWORD=yourpassword
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

### Render deployment and social sign-in

The included `Dockerfile` builds the Java 25 application for Render. Create a **Web Service**
from this folder and select the Docker runtime. The demo database seeds itself on first start.

Email/password sign-in and the demo accounts work without provider credentials. To enable the
Google and Apple buttons, add these secrets under the Render service's Environment settings:

| Render variable | Value |
|---|---|
| `GOOGLE_CLIENT_ID` | Google OAuth web client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth web client secret |
| `APPLE_CLIENT_ID` | Apple Services ID for the website |
| `APPLE_CLIENT_SECRET` | Apple client-secret JWT generated with the Sign in with Apple key |

Register these callback URLs with the providers after the Render service has a hostname:

```text
https://YOUR-SERVICE.onrender.com/login/oauth2/code/google
https://YOUR-SERVICE.onrender.com/login/oauth2/code/apple
```

Set up only the provider(s) you want to use. The buttons report which provider is missing its
credentials. The interactive map uses OpenStreetMap tiles and the location pins returned by the
backend, so map tiles need an internet connection. Event entries are seeded by the backend; saved
events are kept in the current browser on that device.

### If `java` is not JDK 25

`tools/setup-jdk.sh` downloads and installs Temurin JDK 25 (no-op when it is already there):

```bash
bash tools/setup-jdk.sh
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
./mvnw spring-boot:run
```

### Opening it in an IDE

IntelliJ IDEA is recommended (Ultimate free with a student licence; Community is sufficient).
VS Code, SDK/JDK setup and the usual "port in use / permission denied" fixes are in
**`docs/ide-setup.md`**.

### Run the tests

```bash
./mvnw test          # 76 tests: GeoUtils, ZoneService, Scoring, OpeningHours, discovery,
                     #            reports/backup, HTTP + JWT + e-mail verification
```

### Build a runnable jar

```bash
./mvnw clean package
java -jar target/cbdventures-0.0.1-SNAPSHOT.jar
```

---

## 3. Demo accounts

All seeded accounts share the password **`Password123!`** except the administrator.

| Role | Email | Password | What to try |
|---|---|---|---|
| User / visitor | `keziah@student.richfield.ac.za` | `Password123!` | Review a listing, save preferences |
| Business owner | `thabo@kotakingdom.co.za` | `Password123!` | Owner dashboard, add/edit listings, publish an event |
| Administrator | `admin@cbdventures.co.za` | `Admin123!` | Approve or reject the verification queue (seeded with 2 pending submissions) |

Passwords are stored as **BCrypt** hashes; the plain text is never persisted or returned by any
endpoint. You can also register a fresh account from the UI.

---

## 4. Implemented features (mapped to the phase documents)

### Functional requirements

| Ref | Requirement | Where it lives |
|---|---|---|
| FR-01 | Filter by category, price range, proximity (1 km) and rating; sort by distance, price or reviews | `GET /api/businesses` → `BusinessService.search`, `BusinessQuery`, `BusinessSpecifications` |
| FR-02 | Essential listing details cached for offline access | `static/js/api.js` (localStorage cache) + `static/sw.js` (service worker) |
| FR-03 | Business owner portal: register, create, edit and delete own listing | `GET/POST/PUT/DELETE /api/owner/businesses` → `OwnerController`, `BusinessService` |
| FR-04 | Personalised recommendations from saved preferences and activity | `RecommendationService`, `Scoring`, `GET /api/recommendations`, `/api/preferences` |
| — | Management reporting (5 reports, CSV export) | `ReportService`, `GET /api/admin/reports/**`, UI `views/reports.js` |
| — | System backup and restore | `BackupService`, `GET /api/admin/backup`, `POST /api/admin/backup/restore`, UI `views/backup.js` |

### Use cases from the SRS

* **Searching for a business** — `/api/businesses?q=` matches names, descriptions, addresses,
  category names and amenity names; empty results suggest another category.
* **Browsing by category** — five curated categories, each tile counting live listings.
* **Filtering by location** — every distance is measured from the Richfield anchor (or the
  visitor's device position when they tap *Use my location*); the browser falls back to campus
  distances with a note when location is unavailable.
* **Viewing a business profile** — images, description, category, price level, hours, contact
  details, map pin, directions link, amenities, events and reviews. A missing field shows
  "not available" for that field only.
* **Checking a destination** — after a user opens a place's Directions link and returns to the
  assistant, it asks whether the mapped location was accurate. The answer is saved in that
  browser on that device; the app does not use GPS to claim that the person arrived.
* **Leaving a review** — one review per user per business, editable once every 24 hours
  (HTTP 429 outside the window), owners blocked from reviewing their own business, average rating
  recalculated from stored reviews.

### Non-functional requirements

* **Performance** — a single aggregated rating query per result set (no N+1), gzip compression,
  static asset caching, service-worker shell caching.
* **Security** — JWT (HS256) bearer tokens, BCrypt hashing, stateless sessions, role-based
  authorisation (VISITOR / OWNER / ADMIN), POPIA data minimisation (name + email only).
* **Usability** — responsive from 360 px phones to wide desktops, bottom tab bar on mobile,
  plain-English error messages, keyboard-accessible dialogs, skip link, ARIA labels,
  `prefers-reduced-motion` support.
* **Reliability** — friendly JSON errors instead of stack traces, cached listings when offline,
  an offline banner driven by a live connection probe.
* **Scalability** — the 1 km → 5 km expansion is a configuration flag (`cbd.zone.extended-enabled`),
  no schema change: listings are already classified `PRIMARY_1KM` or `EXTENDED_5KM`.
* **Maintainability** — clear package separation (`domain`, `repo`, `service`, `web`, `config`,
  `security`, `error`), categories and amenities are data not code.

### Business rules enforced in code

| Rule (Phase 3) | Implementation |
|---|---|
| Listing must sit inside the pilot zone | `ZoneService.classify` (Haversine) — outside 5 km returns HTTP 400 with the measured distance |
| Extended-zone listings stay hidden until enabled | `ZoneService.isDiscoverable`, `BusinessSpecifications.discoverable` |
| New listings await verification before publishing | status starts `PENDING`; only `ACTIVE` listings are discoverable; `AdminService.decide` approves/rejects |
| One active review per user per business | unique constraint `uk_review_user_business` + `ReviewService.upsert` |
| 24-hour review cooldown (anti-spam) | `ReviewService` cooldown check → HTTP 429 |
| Owner cannot review their own business | `ReviewService.upsert` → HTTP 403 |
| Rating is a whole number 1–5 | Bean Validation + database `CHECK (rating BETWEEN 1 AND 5)` |
| Opening hours per weekday, closed days allowed | `business_hours` (UNIQUE per business+day) + `BusinessService.buildHours`, with `business.open_time`/`close_time` kept as a documented fallback |
| Per-day time rules: open ⇒ both times, opening before closing, closed ⇒ no times | Java validation + `chk_hours_window` + triggers `trg_hours_window_insert/_update` |
| Same rule enforced for direct SQL writers | 8 MySQL triggers in `db/triggers_mysql.sql` (zone, self-review, review edit window, future events, hours) |
| Exactly one primary image per business | `BusinessService.applyImages` flags the first image |
| Events belong to a listed venue, must be in the future | `EventListing.business` NOT NULL + `@Future` + cascade delete |
| Cascade/RESTRICT delete policy | JPA cascade on children, `RESTRICT` on `app_user` (owner) and `category` |
| Recommendations reflect what a visitor browsed | `user_activity` (VIEW_PROFILE / SEARCH / SAVE_BUSINESS) → `ActivityService.affinityWeights`, de-duplicated within 30 minutes |

---

## 5. API reference

Base path `/api`. All responses are JSON. Errors share one shape:

```json
{ "timestamp": "2026-10-26T09:24:14Z", "status": 400, "error": "Bad Request",
  "message": "This address is 52.9 km from Richfield, which is outside the 5 km CBD boundary.",
  "path": "/api/owner/businesses" }
```

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | – | Create a VISITOR or OWNER account, returns a JWT |
| POST | `/auth/login` | – | Exchange email + password for a JWT |
| GET | `/auth/me` | Bearer | Current profile (204 when signed out) |
| POST | `/auth/password` | Bearer | Change password (min 8 chars, at least one digit) |

Send the token as `Authorization: Bearer <token>`.

### Discovery (public)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/discover/home?lat&lng` | Category tiles, featured, personalised suggestions, events, stats |
| GET | `/businesses` | Search/filter/sort. Parameters: `q`, `category`, `price` (1=R, 2=RR, 3=RRR, repeatable), `minRating`, `maxDistanceKm`, `amenity` (repeatable), `openNow`, `sort` (`recommended`\|`distance`\|`price`\|`rating`\|`name`), `page`, `size`, `lat`, `lng` |
| GET | `/businesses/{id}` | Full profile including reviews, amenities, events and zone distance |
| GET | `/businesses/{id}/reviews` | Reviews for a listing |
| POST | `/businesses/{id}/reviews` | Create or update your review (`{ "rating": 1-5, "comment": "…" }`) |
| DELETE | `/businesses/reviews/{reviewId}` | Delete your own review |
| GET | `/categories`, `/amenities`, `/meta/filters`, `/meta/zone`, `/meta/price-levels` | Reference data for the UI |
| GET | `/events?category&maxDistanceKm&limit` | Upcoming events in the zone |
| GET | `/recommendations?lat&lng&limit` | Personalised list (public, personalised when signed in) |
| GET/PUT | `/preferences` | Read/save saved preferences (FR-04, bearer) |

### Owner portal

| Method | Endpoint | Description |
|---|---|---|
| GET | `/owner/businesses` | Your listings with status, rating and review count |
| POST | `/owner/businesses` | Create a listing (saved as `PENDING`) |
| PUT | `/owner/businesses/{id}` | Update a listing (re-enters verification) |
| DELETE | `/owner/businesses/{id}` | Delete a listing (cascades to images, events, reviews) |
| GET/POST | `/owner/businesses/{id}/events` | List/add events at your venue |
| DELETE | `/owner/events/{eventId}` | Remove an event |

### Administrator

| Method | Endpoint | Description |
|---|---|---|
| GET | `/admin/businesses?status=PENDING` | Verification queue |
| POST | `/admin/businesses/{id}/approve` | Publish a listing (optional `{ "note": "…" }`) |
| POST | `/admin/businesses/{id}/reject` | Reject with a note to the owner |
| GET | `/admin/stats` | Platform statistics by status and category |
| GET | `/admin/reports` | Catalogue of the five management reports |
| GET | `/admin/reports/{key}` | One report as a table (`columns`, `rows`, `metrics`) |
| GET | `/admin/reports/{key}/csv` | The same report as a CSV download |
| GET | `/admin/backup/summary` | Row counts of what a backup would contain |
| GET | `/admin/backup` | Full JSON backup of all ten tables (download) |
| POST | `/admin/backup/restore` | Restore a backup (raw JSON body, single transaction) |

---

## 6. Administrator modules: reports and system backup

Both are reachable from the verification queue header and from each other.

### Reports — `#/reports` (5 reports, CSV export)

| Report | Answers |
|---|---|
| Listings by category | Live / pending / rejected counts and average rating per category |
| Verification audit | Every submission, its owner, status, administrator note, submitted date and distance |
| Review activity | Review volume, average and the 1–5 star spread per live listing |
| Owner performance | Listings, live listings, events hosted and reviews received per owner |
| Events schedule | Chronological calendar with ticket price and distance from campus |

Each report shows four headline metrics and downloads as CSV (`GET /api/admin/reports/{key}/csv`).
Administrator-only: guests receive **401**, signed-in non-admins **403**.

### System backup — `#/backup`

* Shows what the database holds (listings, reviews, accounts, events, categories, images).
* **Download backup** writes every table to one JSON file (`cbdventures-backup` v1) —
  including BCrypt hashes, so accounts still sign in after a restore.
* **Restore from a backup** replaces the data in a *single transaction*; a corrupt file
  changes nothing. Everyone is signed out afterwards because ids are reassigned.
* Database-level alternatives (`mysqldump`, H2 file copy) are documented in
  [`docs/backup-and-restore.md`](docs/backup-and-restore.md).

## 7. Project structure

```
cbdventures/
├── db/
│   ├── schema_mysql.sql              Physical MySQL 8 schema: 12 tables, 15 FKs, 21 indexes
│   ├── triggers_mysql.sql           8 server-side integrity triggers
│   └── tests/trigger_tests.sql      20 database rule tests (rolled back at the end)
├── docs/
│   ├── api.md                        Endpoint reference with examples
│   ├── requirements-traceability.md  Every SRS requirement → code location
│   ├── database-design.md            Tables, integrity rules, triggers, merge rationale
│   ├── backup-and-restore.md         Backup runbook (JSON, mysqldump, H2 copy)
│   └── evaluation-compliance.md      Evaluation sheet → evidence mapping
├── src/main/java/za/ac/richfield/cbdventures/
│   ├── CbdventuresApplication.java
│   ├── config/       SecurityConfig, WebConfig, ZoneProperties, JwtProperties, DataSeeder
│   ├── domain/       User, Category, Business, BusinessImage, BusinessHours, Amenity,
│   │                 EventListing, Review, UserPreference, UserActivity
│   │                 + enums (Role, PriceLevel, BusinessStatus, ZoneRadius, ActivityType)
│   ├── repo/         Spring Data repositories + BusinessSpecifications
│   ├── service/      AuthService, BusinessService, ReviewService, EventService,
│   │                 RecommendationService, AdminService, ZoneService, BusinessMapper,
│   │                 Scoring, ReportService, BackupService, ActivityService
│   ├── web/          REST controllers + DTO records
│   ├── security/     JwtService (dependency-free HS256), JwtAuthenticationFilter, CurrentUser
│   ├── error/        ApiException, GlobalExceptionHandler
│   └── util/         GeoUtils (Haversine distance)
├── src/main/resources/
│   ├── application.yml              Demo profile (H2)
│   ├── application-mysql.yml        Production profile (MySQL)
│   └── static/                      Web client: index.html, css/app.css, js/*, images/*, sw.js, manifest
├── src/test/java/…                  73 JUnit tests
└── tools/generate_artwork.py        Regenerates the SVG illustration set
```

---

## 8. Project-review tooling (internal, not part of the submission)

Two helper scripts score a codebase against the Richfield evaluation sheet by looking for
evidence on disk. They are development aids — excluded from the submission ZIP.

```bash
# side-by-side comparison of two codebases
python3 tools/compare_projects.py --mine . --theirs /path/to/other/repo --out comparison.md

# review one codebase and get a prioritised fix list
python3 tools/review_repo.py --path /path/to/other/repo --out review.md
```

`docs/repo-comparison-baseline.md` is this project's own profile (80/80 indicative).

## 9. Design notes

**Why a hash-router SPA?** One origin, one port, no Node build step — the entire application can
be demonstrated with `./mvnw spring-boot:run` on any machine with a JDK, which matches the
"free tier / run locally for demos" constraint in the SRS.

**Why H2 file mode by default?** The markers can open the project with no database install, while
`db/schema_mysql.sql` + the `mysql` profile prove the production path. Both use the same entities.

**Why a hand-written JWT service?** It keeps the dependency surface small (JDK `javax.crypto`
only) and makes the token format inspectable for the report. Spring Security still performs the
authorisation; the filter only establishes identity.

**Why distance in Java rather than a spatial database?** Haversine works identically on H2 and
MySQL and keeps the schema portable. At pilot scale (≈50–150 listings) the cost is negligible;
`db/schema_mysql.sql` documents the equivalent SQL aggregate for a larger data set.

**Why per-day hours *and* triggers?** A single `open_time`/`close_time` pair cannot express
"open Monday to Saturday, closed on Sunday", so hours moved into a `business_hours` table with
one row per weekday. Doing that also means "is it open now?" has a single source of truth, which
is why the flat columns remain only as a fallback for older clients. The triggers are a second
line of defence for the same rules, so a listing edited directly in SQL is still held to the
1 km zone. See `docs/database-design.md`.

**Accessibility & offline.** All icons are inline SVG, all photography is generated vector art
(`tools/generate_artwork.py`), so nothing depends on third-party CDNs; the client therefore works
offline and inside sandboxed previews.

---

## 10. Credit: what came from the group's database design

This project's schema and the schema in the group's `CBDVentures` repository were merged.
Three ideas from their design are now part of this build, and are documented in
`docs/database-design.md`:

1. **Database triggers** for the 1 km zone, future events, self-review and the 24-hour review
   window (`db/triggers_mysql.sql`). Two were tightened while adopting them.
2. **`business_hours`**, one row per business per weekday, replacing a flat single window.
3. **`user_activity`**, the browsing trail now used by the recommendation engine.

Two corrections were applied: the brief's "one review per user per business" became a
`UNIQUE (user_id, business_id)` constraint (their trigger only limited repeat reviews to one a
day), and `business_hours` gained an open-before-close CHECK plus `NOT NULL` on
`business.owner_id`, which their own integrity document listed as outstanding.

---

## 11. Known limitations (post-MVP backlog)

* Payment/reservation integration is out of scope (Phase 2 explicitly excludes it).
* Push notifications are not implemented (listed as out of scope).
* The native Android client is not part of this repository; the web client is a responsive
  installable PWA (`manifest.webmanifest` + service worker), the approach the SRS recommends for a
  one-semester scope.
* The 5 km extended zone is implemented and switchable, but its data set is intentionally empty.
