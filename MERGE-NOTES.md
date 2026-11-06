# Merge notes — final consolidated folder

Built from `CBDVentures-DarkMode-FINAL.zip` (the base) plus every tracked file in
https://github.com/Coding-Heaven-Peta/CBDVentures (branches `main`, `CBDVentures-database-handover`
and `Testing-Phase` — the two branches contain only files that are also on `main`).

No file in the repo was byte-identical to a file in the zip, and no repo file was dropped as a
duplicate. Nothing in the zip was changed except `.gitignore` (map-module entries appended).
The application source, tests, `db/schema_mysql.sql`, `db/triggers_mysql.sql` and the existing
`docs/` files are untouched. To run the app, follow `README.md` (`mvnw.cmd spring-boot:run`).

## Where the repo files went

| Repo file(s) | Now at | Notes |
|---|---|---|
| `map_api.py`, `map_builder.py`, `map_core.py`, `requirements.txt`, `.env.example`, `generate_map_files.bat` | `map/Python_Source/` | The `.bat` files expect this layout |
| `test_map_api.py`, `test_map_core.py` | `map/Python_Source/tests/` | 10 tests pass |
| `setup_windows.bat`, `run_map_api.bat` | `map/` | |
| `cbdventures_map_preview.html` | `map/Map_Demo/` | |
| `businesses.geojson` | `map/Map_Data/` | |
| `README.md` (map handover) | `map/README.md` | Not the project README |
| `cbdventures_db_final_checkpoint.sql`, `cbdventures_db_triggers_backup_all_six.sql`, `README_Import_Instructions.txt` | `db/snapshots/` | Older MySQL export (3 listings) |
| `Backups/*` | `db/snapshots/earlier-backups/` | |
| `Testing and backup/*.sql` | `db/snapshots/testing/` | |
| `Data_Security_and_Integrity.docx`, `Testing_Report.pdf` | `docs/phase4-reports/` | |
| `app.js`, `style.css`, `frontpage.html`, `home.html`, `profile.html`, `signin.html`, `signup.html` | `legacy-frontend/` | Earlier prototype UI; the dark SPA in `src/main/resources/static` replaces it. Not served by Spring |

Left out on purpose: `.idea/` and `cbdventures-api/.idea/` (IDE config; the repo's `cbdventures-api`
folder holds no source), and `.gitignore` (merged into the existing one).

## Open points the repo README raised

1. **Backend choice** — settled by this build: Java 25 / Spring Boot with H2 (demo) or MySQL.
   The Python map module is an optional extra and the app does not depend on it.
2. **Campus coordinates differ** — snapshots/map module use -25.746400, 28.192483; the Spring app
   (`application.yml`) uses -25.746350, 28.192550 (about 8 m apart). Pick one and align them.
3. **Schema differs** — `db/snapshots/*` use the old table names (e.g. `user`); the app's
   `db/schema_mysql.sql` uses `app_user` etc. Do not mix them.
4. Snapshots contain user records: share only through approved channels.
