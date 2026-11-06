# Backup and restore runbook

> Phase 3 deliverable: **System Backup**.
> CBDVentures supports two complementary backup strategies. Use the JSON backup for
> demonstrations and moving data between machines; use the database dump for a real
> production deployment.

---

## 1. JSON backup (built into the application)

Administrator screen: **`#/backup`** (linked from the verification queue and the reports page).

| Action | Endpoint | Who |
|---|---|---|
| See what the database holds | `GET /api/admin/backup/summary` | ADMIN |
| Download a full backup | `GET /api/admin/backup` | ADMIN |
| Restore a backup | `POST /api/admin/backup/restore` (raw JSON body) | ADMIN |

### What is inside the file

One JSON document, format marker `cbdventures-backup`, version `1`:

```json
{
  "format": "cbdventures-backup",
  "version": 1,
  "generatedAt": "2026-10-27T21:05:23+02:00[Africa/Johannesburg]",
  "generatedBy": "admin@cbdventures.co.za",
  "counts": { "categories": 5, "amenities": 14, "users": 17, "businesses": 29,
              "images": 56, "events": 12, "reviews": 45, "preferences": 0 },
  "categories": [ … ],   "amenities": [ … ],  "users": [ … ],
  "businesses": [ … ],   "events": [ … ],     "reviews": [ … ],
  "preferences": [ … ]
}
```

Coverage: **all ten tables** — `category`, `amenity`, `app_user`, `business`,
`business_amenity`, `business_image`, `event`, `review`, `user_preference`,
`preference_category`.

Business rows include their amenity names and image list, so a restore rebuilds the
many-to-many and one-to-many relationships without needing the original ids.

### Guarantees

* **Single transaction.** The restore empties and reloads inside one transaction; if any
  row is invalid the whole restore is rolled back and the database is untouched.
* **Passwords still work.** BCrypt hashes are exported verbatim, so every account can sign
  in with its original password after a restore.
* **Signed out afterwards.** Restored rows receive new internal ids, so existing sessions no
  longer match. The UI automatically signs everyone out and asks them to sign in again.
* **Validated input.** Wrong format marker, missing categories or a newer schema version are
  rejected with a friendly 400 message.

> **POPIA:** the file contains account records (names, emails, password hashes). Store it
> like any other personal-information export and delete copies you no longer need.

### Command line

```bash
# 1. get a token
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@cbdventures.co.za","password":"Admin123!"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')

# 2. download
curl -s localhost:8080/api/admin/backup \
  -H "Authorization: Bearer $TOKEN" -o cbdventures-backup.json

# 3. restore (on any machine running the same or newer version)
curl -s -X POST localhost:8080/api/admin/backup/restore \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data-binary @cbdventures-backup.json
```

---

## 2. Database dump (production / demo file copy)

### MySQL 8 (the `mysql` profile)

```bash
# backup
mysqldump -u root -p --single-transaction --routines cbdventures > cbdventures-$(date +%F).sql

# restore
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS cbdventures"
mysql -u root -p cbdventures < cbdventures-2026-10-27.sql
```

Schedule it nightly (example crontab entry, 02:00):

```cron
0 2 * * * mysqldump -u root -p"$DB_PASSWORD" --single-transaction cbdventures \
  | gzip > /var/backups/cbdventures-$(date +\%F).sql.gz
```

Keep the last 7 daily copies:

```bash
find /var/backups -name 'cbdventures-*.sql.gz' -mtime +7 -delete
```

### H2 (the default demo profile)

The whole database is one file: `data/cbdventures.mv.db`.

```bash
# stop the server first, then:
cp data/cbdventures.mv.db backups/cbdventures-$(date +%F).mv.db

# restore
cp backups/cbdventures-2026-10-27.mv.db data/cbdventures.mv.db
```

Deleting `data/` entirely and restarting re-creates the seeded demo data set.

---

## 3. Recommended routine for the project demo

1. Before the presentation: **`#/backup` → Download backup** and keep the `.json` in the
   project folder.
2. Show the marker the summary cards (29 listings, 45 reviews, 17 accounts, …).
3. If anything goes wrong during the demo: **Restore from a backup**, choose the file and
   confirm. The database returns to its saved state in about a second.
4. Sign in again as administrator (`admin@cbdventures.co.za` / `Admin123!`).

---

## 4. Verification checklist

| Check | How |
|---|---|
| Backup endpoint is admin-only | `curl` without a token returns **401** |
| Backup covers every table | Compare `counts` with `SELECT COUNT(*)` on each table |
| Restore is atomic | Upload a corrupt file — the data is unchanged and a 400 message is shown |
| Passwords survive | Restore, then sign in with a seeded account |
| Round trip is lossless | `ReportingTest.backupRestoreRoundTrip` asserts identical row counts |
