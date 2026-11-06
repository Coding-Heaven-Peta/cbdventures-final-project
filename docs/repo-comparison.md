# CBDVentures — project comparison against the evaluation sheet

- **Mine:** `cbdventures` (166 files)
- **Theirs:** `theirs` (11 files)

## Stack detected

| Language | Mine | Theirs |
|---|---|---|
| css | 1 | 0 |
| html | 1 | 0 |
| java | 70 | 0 |
| js_ts | 15 | 0 |
| python | 2 | 0 |
| sql | 1 | 6 |

## Evaluation standards

| Standard | Marks | Mine | Theirs | Gap |
|---|---|---|---|---|
| Running System | 10 | 10.0/10 | 0.0/10 | +10.0 🔵 mine ahead |
| Validation | 10 | 10.0/10 | 2.5/10 | +7.5 🔵 mine ahead |
| Database | 10 | 10.0/10 | 6.0/10 | +4.0 🔵 mine ahead |
| Form Links | 10 | 10.0/10 | 0.0/10 | +10.0 🔵 mine ahead |
| System Backup | 10 | 10.0/10 | 10.0/10 | +0.0  |
| Reports (3-5) | 10 | 10.0/10 | 5.0/10 | +5.0 🔵 mine ahead |
| Login and Security | 10 | 10.0/10 | 5.0/10 | +5.0 🔵 mine ahead |
| User Friendliness | 5 | 5.0/5 | 1.2/5 | +3.8 🔵 mine ahead |
| Modules (at least 2) | 5 | 5.0/5 | 1.7/5 | +3.3 🔵 mine ahead |

**Indicative total: 80.0/80** (mine) vs **31.4/80** (theirs)

## Evidence per check

| Check | Standard | Mine | Theirs |
|---|---|---|---|
| Build file (pom/gradle/package/requirements) | Running System | yes | **no** |
| Application entry point / server bootstrap | Running System | yes | **no** |
| Run instructions documented | Running System | yes | **no** |
| HTML/CSS/JS front end present | Running System | yes | **no** |
| Server-side validation annotations | Validation | yes | **no** |
| Client-side validation (required/pattern/JS checks) | Validation | yes | yes |
| Central error handling with readable messages | Validation | yes | **no** |
| Business rules enforced in code (not just UI) | Validation | yes | **no** |
| SQL schema / migration file | Database | yes | yes |
| Entity/model classes | Database | yes | **no** |
| Foreign keys / relationship mapping | Database | yes | yes |
| Integrity constraints (UNIQUE/CHECK/ENUM/NOT NULL) | Database | yes | yes |
| Indexes defined | Database | yes | **no** |
| HTML forms | Form Links | yes | **no** |
| Forms wired to backend calls | Form Links | yes | **no** |
| CRUD endpoints (create/update/delete) | Form Links | yes | **no** |
| Backup feature | System Backup | yes | yes |
| Restore / recovery path | System Backup | yes | yes |
| Report generation | Reports (3-5) | yes | yes |
| CSV / printable export | Reports (3-5) | yes | **no** |
| Login / authentication | Login and Security | yes | **no** |
| Password hashing (not plain text) | Login and Security | yes | yes |
| Role-based authorisation | Login and Security | yes | **no** |
| Guarded routes / 401-403 handling | Login and Security | yes | yes |
| Responsive layout (media queries) | User Friendliness | yes | **no** |
| Accessibility affordances | User Friendliness | yes | **no** |
| Offline / PWA support | User Friendliness | yes | **no** |
| Toasts / loaders / empty states | User Friendliness | yes | yes |
| Automated tests | Modules (at least 2) | yes | yes |
| Written documentation | Modules (at least 2) | yes | **no** |
| Screenshots / evidence pack | Modules (at least 2) | yes | **no** |

## What only one project has

**Only mine:** Build file (pom/gradle/package/requirements), Application entry point / server bootstrap, Run instructions documented, HTML/CSS/JS front end present, Server-side validation annotations, Central error handling with readable messages, Business rules enforced in code (not just UI), Entity/model classes, Indexes defined, HTML forms, Forms wired to backend calls, CRUD endpoints (create/update/delete), CSV / printable export, Login / authentication, Role-based authorisation, Responsive layout (media queries), Accessibility affordances, Offline / PWA support, Written documentation, Screenshots / evidence pack

**Only theirs:** _nothing_


---
### How to read the score

The score is a **floor, not a mark**. It only counts detectable evidence on disk; a human marker also judges demo quality, data realism and explanation. Treat `no` as "nothing found here — check by hand before claiming it".
