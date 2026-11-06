# CBDVentures — project comparison against the evaluation sheet

- **Profile:** `cbdventures` (164 files)

## Stack detected

| Language | Mine | Theirs |
|---|---|---|
| css | 1 | — |
| html | 1 | — |
| java | 70 | — |
| js_ts | 15 | — |
| python | 1 | — |
| sql | 1 | — |

## Evaluation standards

| Standard | Marks | Evidence found | Checks passed | Indicative score |
|---|---|---|---|---|
| Running System | 10 | 4/4 | 100% | 10.0/10 |
| Validation | 10 | 4/4 | 100% | 10.0/10 |
| Database | 10 | 5/5 | 100% | 10.0/10 |
| Form Links | 10 | 3/3 | 100% | 10.0/10 |
| System Backup | 10 | 2/2 | 100% | 10.0/10 |
| Reports (3-5) | 10 | 2/2 | 100% | 10.0/10 |
| Login and Security | 10 | 4/4 | 100% | 10.0/10 |
| User Friendliness | 5 | 4/4 | 100% | 5.0/5 |
| Modules (at least 2) | 5 | 3/3 | 100% | 5.0/5 |

**Indicative total: 80.0/80**

## Evidence per check

| Check | Standard | Present | Where |
|---|---|---|---|
| Build file (pom/gradle/package/requirements) | Running System | yes | `pom.xml` |
| Application entry point / server bootstrap | Running System | yes | `src/main/java/za/ac/richfield/cbdventures/CbdventuresApplication.java, tools/generate_artwork.py` |
| Run instructions documented | Running System | yes | `README.md, PACKAGING.md, db/schema_mysql.sql` |
| HTML/CSS/JS front end present | Running System | yes | `src/main/resources/static/index.html` |
| Server-side validation annotations | Validation | yes | `src/main/java/za/ac/richfield/cbdventures/web/dto/AuthDtos.java, src/main/java/za/ac/richfield/cbdventures/web/dto/AdminDtos.java, src/main/java/za/ac/richfield/cbdventures/web/dto/EventDtos.java` |
| Client-side validation (required/pattern/JS checks) | Validation | yes | `src/main/resources/application.yml, src/main/resources/static/js/views/auth.js, src/main/resources/static/js/views/owner.js` |
| Central error handling with readable messages | Validation | yes | `src/main/java/za/ac/richfield/cbdventures/error/GlobalExceptionHandler.java` |
| Business rules enforced in code (not just UI) | Validation | yes | `README.md, db/schema_mysql.sql, src/main/resources/static/js/app.js` |
| SQL schema / migration file | Database | yes | `db/schema_mysql.sql, docs/backup-and-restore.md` |
| Entity/model classes | Database | yes | `src/main/java/za/ac/richfield/cbdventures/domain/User.java, src/main/java/za/ac/richfield/cbdventures/domain/Review.java, src/main/java/za/ac/richfield/cbdventures/domain/Amenity.java` |
| Foreign keys / relationship mapping | Database | yes | `README.md, db/schema_mysql.sql, src/main/resources/static/js/views/backup.js` |
| Integrity constraints (UNIQUE/CHECK/ENUM/NOT NULL) | Database | yes | `README.md, db/schema_mysql.sql, src/main/java/za/ac/richfield/cbdventures/domain/User.java` |
| Indexes defined | Database | yes | `src/main/java/za/ac/richfield/cbdventures/domain/Review.java, src/main/java/za/ac/richfield/cbdventures/domain/Business.java, src/main/java/za/ac/richfield/cbdventures/domain/EventListing.java` |
| HTML forms | Form Links | yes | `src/main/resources/static/js/views/home.js, src/main/resources/static/js/views/auth.js, src/main/resources/static/js/views/owner.js` |
| Forms wired to backend calls | Form Links | yes | `src/main/resources/static/sw.js, src/main/resources/static/js/app.js, src/main/resources/static/js/api.js` |
| CRUD endpoints (create/update/delete) | Form Links | yes | `src/main/java/za/ac/richfield/cbdventures/web/AuthController.java, src/main/java/za/ac/richfield/cbdventures/web/OwnerController.java, src/main/java/za/ac/richfield/cbdventures/web/AdminController.java` |
| Backup feature | System Backup | yes | `README.md, PACKAGING.md, db/schema_mysql.sql` |
| Restore / recovery path | System Backup | yes | `README.md, src/main/resources/static/js/api.js, src/main/resources/static/css/app.css` |
| Report generation | Reports (3-5) | yes | `README.md, src/main/resources/static/js/app.js, src/main/resources/static/js/api.js` |
| CSV / printable export | Reports (3-5) | yes | `src/main/resources/static/js/api.js, src/main/resources/static/js/views/reports.js, src/main/java/za/ac/richfield/cbdventures/web/AdminReportingController.java` |
| Login / authentication | Login and Security | yes | `README.md, db/schema_mysql.sql, src/main/resources/application.yml` |
| Password hashing (not plain text) | Login and Security | yes | `README.md, db/schema_mysql.sql, src/main/resources/static/js/app.js` |
| Role-based authorisation | Login and Security | yes | `src/main/resources/static/js/api.js, src/main/java/za/ac/richfield/cbdventures/config/SecurityConfig.java, src/main/java/za/ac/richfield/cbdventures/service/BusinessMapper.java` |
| Guarded routes / 401-403 handling | Login and Security | yes | `README.md, src/main/resources/static/js/app.js, src/main/resources/static/js/api.js` |
| Responsive layout (media queries) | User Friendliness | yes | `src/main/resources/static/js/app.js, src/main/resources/static/index.html, src/main/resources/static/css/app.css` |
| Accessibility affordances | User Friendliness | yes | `README.md, src/main/resources/static/js/ui.js, src/main/resources/static/js/app.js` |
| Offline / PWA support | User Friendliness | yes | `README.md, src/main/resources/static/sw.js, src/main/resources/application.yml` |
| Toasts / loaders / empty states | User Friendliness | yes | `src/main/resources/static/js/ui.js, src/main/resources/static/js/app.js, src/main/resources/static/js/api.js` |
| Automated tests | Modules (at least 2) | yes | `README.md, src/test/java/za/ac/richfield/cbdventures/web/ApiHttpTest.java, src/test/java/za/ac/richfield/cbdventures/util/GeoUtilsTest.java` |
| Written documentation | Modules (at least 2) | yes | `README.md, PACKAGING.md, docs/api.md` |
| Screenshots / evidence pack | Modules (at least 2) | yes | `screenshots/01-home.png, screenshots/05-about.png, screenshots/04-events.png` |


---
### How to read the score

The score is a **floor, not a mark**. It only counts detectable evidence on disk; a human marker also judges demo quality, data realism and explanation. Treat `no` as "nothing found here — check by hand before claiming it".
