# Requirements traceability — Phase 1/2/3 requirements → Phase 3 implementation

This matrix closes the loop on the Requirements Traceability Matrix (RTM) started in the Planning
Phase: every requirement, business rule and non-functional target from the Phase 1 scope document,
the Phase 2 SRS and the Phase 3 analysis is listed with the artefact that implements it and the
evidence that it works.

Legend: **API** = REST endpoint · **Class** = server-side implementation · **UI** = web client ·
**Test** = automated test that proves the behaviour.

---

## 1. Functional requirements

| Ref | Requirement (source) | Implementation | Evidence |
|---|---|---|---|
| FR-01 | Filter listings by category, price range, proximity (1 km) and ratings | `BusinessService.search`, `BusinessQuery`, `BusinessSpecifications`, `ZoneService` · `GET /api/businesses` | `DiscoveryIntegrationTest.priceFilter`, `.distanceFilter`, `.minRatingFilter`, `.browseByCategory`, `.amenityFilter` |
| FR-01a | Sort by distance, price or reviews | `BusinessService.sort` (comparator per sort key) | `DiscoveryIntegrationTest.sortByDistance`, `.sortByPrice` |
| FR-02 | Essential listing details cached locally for offline access | `static/js/api.js` (read-through localStorage cache, 12 h TTL), `static/sw.js` (network-first with cache fallback), offline banner in `app.js` | Manual: DevTools → Offline; UI banner + cached listings |
| FR-03 | Business owner portal to register, create and maintain profiles | `OwnerController`, `BusinessService.create/update/delete`, `AuthService.register` (OWNER role) · UI `views/owner.js` | `ApiHttpTest.zoneValidationOverHttp`; browser flow: register → add listing → edit → events |
| FR-04 | Personalised recommendations from saved preferences and user activity | `RecommendationService` (preference weights + review history), `Scoring`, `GET /api/recommendations`, `GET/PUT /api/preferences` | `DiscoveryIntegrationTest.preferencesPersonaliseFeed`, `ScoringTest.*` |
| — | Search by name or keyword | `BusinessSpecifications.keywords` (name, description, address, category, amenities) | `DiscoveryIntegrationTest.keywordSearch` |
| — | Browse by the five categories | `CatalogController`, category tiles in `views/home.js` | `ApiHttpTest.publicDiscovery` |
| — | View a full business profile with map pin and directions | `GET /api/businesses/{id}`, `BusinessMapper.toDetail`, inline SVG zone map (`ui.js`), Google Maps directions link | UI profile screen; `ApiHttpTest.publicDiscovery` |
| — | Leave a review and recalculate the average | `ReviewService.upsert`, `BusinessMapper.ratingsFor` (single aggregate query) | `DiscoveryIntegrationTest.reviewRecalculatesAverage` |
| — | Business owner accounts manage only their own listings | Ownership checks in `BusinessService.update/delete`, `EventService`; `SecurityConfig` role rules | Manual: second owner receives 403 |
| — | Admin verifies listings before publishing | `AdminService.decide`, `AdminController`, status `PENDING → ACTIVE` | `DiscoveryIntegrationTest.adminApprovalPublishesListing`, browser admin queue |
| — | **Management reports (3–5 required)** | `ReportService` (five reports), `AdminReportingController`, UI `views/reports.js` | `ReportingTest` (catalogue, totals, CSV, formatting) |
| — | **System backup and restore** | `BackupService`, `GET /api/admin/backup`, `POST /api/admin/backup/restore`, UI `views/backup.js`, `docs/backup-and-restore.md` | `ReportingTest.backupRestoreRoundTrip`, `.backupExportsEveryTable`, `.invalidBackupIsRejected` |

## 2. Use-case acceptance criteria (SRS §3.6)

| Use case | Expected behaviour | Implemented |
|---|---|---|
| Searching for a business | No matches → "No results" plus browse-by-category suggestion | `views/explore.js` empty state with a *Clear filters* action |
| Browsing by category | Empty category → "nothing listed yet" message | `emptyState()` in `ui.js` |
| Filtering by location | Location unavailable → show all pilot-zone results with a note | Distances fall back to the Richfield anchor; the explore subtitle states which reference point is used; a toast explains a declined permission prompt |
| Viewing a business profile | Missing field → "not available" for that field only | `views/business.js` (`openNow: null` → "Hours unknown"; hours/contact fall back individually) |
| Leaving a review | No connection → error message, draft retained | Review modal keeps the textarea contents on failure and shows the API message in an inline notice |

## 3. Data integrity rules (Phase 3 §3.4)

| Rule | Enforced by |
|---|---|
| Entity integrity — every entity has a non-null unique primary key | `@GeneratedValue(IDENTITY)` + `BIGINT` primary keys in every entity and in `db/schema_mysql.sql` |
| Referential integrity — FKs always point at real rows | JPA `@ManyToOne(optional = false)`; MySQL `FOREIGN KEY` constraints |
| `RESTRICT` when deleting an owner or category that still has listings | `fk_business_owner`, `fk_business_category` (`ON DELETE RESTRICT`) |
| `CASCADE` for dependent children (reviews, images, events, amenities) | `CascadeType.ALL + orphanRemoval` on `Business`; `ON DELETE CASCADE` in DDL |
| Domain — rating is an integer 1–5 | Bean Validation `@Min/@Max` + `chk_review_rating` |
| Domain — email format, unique | `@Email`, `uk_user_email`, `chk_user_email` |
| Domain — password min 8 chars, stored hashed | `@Size(min = 8)`, `BCryptPasswordEncoder`, `password_hash VARCHAR(255)` |
| Domain — price level restricted to R/RR/RRR | `PriceLevel` enum + `ENUM('R','RR','RRR')` |
| Domain — opening before closing, both or neither | `BusinessService.apply` + `chk_business_hours` |
| Domain — latitude/longitude within the boundary | `ZoneService.classify` + `chk_business_lat/lng` |
| Domain — review date never in the future, system generated | `createdAt` set by the entity, never accepted from the client |
| Domain — category must be one of the five approved | `category_id` FK to the seeded `category` table |
| Domain — ticket price zero or positive | `@DecimalMin("0.00")` + `chk_event_price` |
| Business rule — listing must be inside the 1 km zone | `ZoneService.classify`; extended zone flagged separately |
| Business rule — one review per user per business | `uk_review_user_business` + `ReviewService.upsert` |
| Business rule — no self-review by the owner | `ReviewService.upsert` → 403 |
| Business rule — no duplicate reviews within 24 h | `ReviewService` cooldown → 429 |
| Business rule — verification before publishing | `PENDING` status + `AdminService` |
| Business rule — exactly one primary image | `BusinessService.applyImages` |
| Business rule — events need a venue and a future date | `@Future`, non-null FK, cascade delete |
| Normalisation (3NF) | `Category`, `Amenity` and `User` referenced by id only; junction tables for the many-to-many relationships |

## 4. Non-functional requirements (SRS §3.7)

| Area | Requirement | Implementation |
|---|---|---|
| Performance | Results within 2–3 s on campus Wi-Fi | Single aggregate rating query per result set (no N+1), JPA specifications pushed to the database, gzip compression, immutable static assets, service-worker shell cache. Typical local response: 20–80 ms for 26 listings |
| Performance | Profile loads within ~3 s on mobile | Server-rendered JSON payload, vector artwork (≈3 KB per image instead of 200 KB photos), lazy-loaded card images |
| Performance | Handles a few dozen concurrent users | Stateless JWT (no server session store), HikariCP pool, no chatty endpoints (home is one round trip) |
| Security | HTTPS | Provided by the hosting platform; HSTS/secure headers available via Spring Security defaults |
| Security | Passwords hashed, never plain | BCrypt (strength 10) via `PasswordEncoder`; verified by `ApiHttpTest.registerAndLogin` (no `password` field in any response) |
| Security | Only a logged-in owner may create/edit/delete their own listing | `SecurityConfig` (`/api/owner/**` requires OWNER/ADMIN) + per-record ownership checks |
| Security | POPIA data minimisation | Only name, email and password hash are collected; no analytics or third-party trackers |
| Security | Protected admin surface | `/api/admin/**` requires `ROLE_ADMIN`; `ApiHttpTest.adminEndpointsAreProtected` |
| Security | Tampered/expired tokens rejected | HMAC-SHA256 signature verification plus `exp` check in `JwtService`; `ApiHttpTest.tamperedTokenIsRejected` |
| Usability | Find a business in ~2 minutes with no instructions | Home search + 5 tiles + suggestions; global search on every screen; "Open right now" shortcut |
| Usability | Responsive on phone and laptop | CSS breakpoints at 420/720/900/1080 px, bottom tab bar under 900 px, sticky filter sheet on mobile |
| Usability | Core actions within one or two taps | Tab bar (Discover / Explore / Events / Business / Account) + card → profile → review |
| Usability | Plain-English labels and errors | `GlobalExceptionHandler` messages, e.g. "This address is 52.9 km from Richfield, which is outside the 5 km CBD boundary." |
| Reliability | Friendly message when the database is unreachable | `GlobalExceptionHandler` returns a JSON message; the client renders an `emptyState` with a retry action |
| Reliability | Previously viewed listings stay visible when the connection drops | localStorage read-through cache + service-worker data cache + offline banner |
| Reliability | Back up before demos | `db/schema_mysql.sql` plus `./data/cbdventures.mv.db` can be copied; re-seeding is deterministic |
| Scalability | Database supports 1 km → 5 km without restructuring | `zone_radius` column (`PRIMARY_1KM` / `EXTENDED_5KM`) + `cbd.zone.extended-enabled` flag |
| Scalability | Pilot scale of 50–150 listings | 26 verified seeded listings plus 3 submissions in the verification queue; `size` capped at 100 per page; indexes on status, category, name, event date |
| Maintainability | Separated layers | `domain` / `repo` / `service` / `web` / `config` / `security` / `error`; the client only talks to `/api` |
| Maintainability | Adding a category means adding data | `category` table drives tiles, filters and validation; no code change needed |
| Maintainability | Documentation kept current | `README.md`, `docs/api.md`, this traceability matrix, Javadoc on every business rule |

## 5. Phase 3 implementation checklist

| Step (Phase 3 §3.8) | Status | Artefact |
|---|---|---|
| 1. Conceptual model (entities, roles, spatial boundaries) | Done | `domain/*`, enums `Role`, `ZoneRadius` |
| 2. Relationships & cardinalities, junction tables | Done | `Business.amenities` (`business_amenity`), `UserPreference.preferredCategories` (`preference_category`) |
| 3. Logical model: attributes, PK/FK | Done | Entity fields + `db/schema_mysql.sql` |
| 4. Normalise to 3NF | Done | Category/Amenity/User referenced by id; 3NF note in `schema_mysql.sql` |
| 5. Physical MySQL schema with constraints | Done | `db/schema_mysql.sql` |
| 6. Visual ERD | Described | See section 6 below (Mermaid renders in GitHub/GitLab) |

## 6. Entity-relationship diagram (Mermaid)

```mermaid
erDiagram
    APP_USER ||--o{ BUSINESS : "owns"
    APP_USER ||--o{ REVIEW : "writes"
    APP_USER ||--o| USER_PREFERENCE : "saves"
    CATEGORY ||--o{ BUSINESS : "classifies"
    CATEGORY ||--o{ AMENITY : "groups"
    CATEGORY ||--o{ PREFERENCE_CATEGORY : "preferred in"
    BUSINESS ||--o{ REVIEW : "receives"
    BUSINESS ||--o{ EVENT : "hosts"
    BUSINESS ||--o{ BUSINESS_IMAGE : "showcases"
    BUSINESS ||--o{ BUSINESS_AMENITY : "features"
    AMENITY ||--o{ BUSINESS_AMENITY : "applied to"
    USER_PREFERENCE ||--o{ PREFERENCE_CATEGORY : "includes"

    APP_USER {
        bigint user_id PK
        varchar full_name
        varchar email UK
        varchar password_hash
        enum role
        timestamp created_at
    }
    CATEGORY {
        bigint category_id PK
        varchar category_name UK
        varchar slug UK
        varchar description
        varchar icon_key
        int sort_order
    }
    BUSINESS {
        bigint business_id PK
        bigint owner_id FK
        bigint category_id FK
        varchar name
        text description
        varchar address
        decimal latitude
        decimal longitude
        enum zone_radius
        enum price_level
        varchar operating_hours
        time open_time
        time close_time
        varchar phone
        varchar whatsapp
        varchar website
        enum status
        varchar admin_note
        timestamp created_at
        timestamp updated_at
    }
    AMENITY {
        bigint amenity_id PK
        bigint category_id FK
        varchar amenity_name UK
        varchar icon_key
    }
    BUSINESS_AMENITY {
        bigint business_id PK_FK
        bigint amenity_id PK_FK
    }
    BUSINESS_IMAGE {
        bigint image_id PK
        bigint business_id FK
        varchar image_url
        varchar caption
        boolean is_primary
    }
    EVENT {
        bigint event_id PK
        bigint business_id FK
        varchar title
        text description
        varchar venue
        datetime event_date_time
        decimal ticket_price
    }
    REVIEW {
        bigint review_id PK
        bigint user_id FK
        bigint business_id FK
        int rating
        text comment
        boolean is_verified
        timestamp created_at
        timestamp updated_at
    }
    USER_PREFERENCE {
        bigint preference_id PK
        bigint user_id FK_UK
        enum max_price_level
        double max_distance_km
    }
    PREFERENCE_CATEGORY {
        bigint preference_id PK_FK
        bigint category_id PK_FK
    }
```

## 7. Phase 4 testing summary

| Layer | Suite | Tests |
|---|---|---|
| Geographic rules | `GeoUtilsTest`, `ZoneServiceTest` | 13 |
| Recommendation scoring | `ScoringTest` | 6 |
| Service + persistence integration | `DiscoveryIntegrationTest` | 18 |
| **Reports and system backup** | `ReportingTest` | 12 |
| HTTP contract, JWT and security | `ApiHttpTest` | 10 |
| **Total** | | **59 passing** |

Run with `./mvnw test`. Coverage focus: every business rule in section 3 that can be asserted
programmatically has a dedicated test, and the HTTP suite proves the authorisation rules (guests may
browse, reviews need a token, admin endpoints are protected, tampered tokens are rejected).
