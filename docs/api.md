# CBDVentures REST API reference

Base URL: `http://localhost:8080/api` · Format: JSON (UTF-8) · Auth: `Authorization: Bearer <JWT>`

Error shape (all failures):

```json
{
  "timestamp": "2026-10-26T09:24:14.199Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Please choose the opening and closing time, or leave both blank.",
  "path": "/api/owner/businesses"
}
```

Status codes used: `200` OK, `201` Created, `204` No Content, `400` validation/business rule,
`401` not signed in, `403` signed in but not allowed, `404` not found, `409` duplicate account,
`429` review cooldown.

---

## Authentication

### POST /auth/register

```json
{ "fullName": "Keziah Chinasamy", "email": "keziah@student.richfield.ac.za",
  "password": "Password123", "role": "VISITOR" }
```

`role` accepts `VISITOR` or `OWNER` (administrators are created by the platform team).

Registration no longer returns a token: the account is created unverified and a six-digit
code is issued (dark-mode build, see `DARK-MODE.md`).

Response `201`:

```json
{ "email": "keziah@student.richfield.ac.za", "fullName": "Keziah Chinasamy",
  "verificationRequired": true, "demoCode": "418302", "expiresInMinutes": 10 }
```

### POST /auth/verify

```json
{ "email": "keziah@student.richfield.ac.za", "code": "418302" }
```

`200` with the usual `AuthResponse` (`token` + `user`, now `emailVerified: true`).
Wrong or expired code → `400`; five wrong attempts invalidate the code.

### POST /auth/resend

```json
{ "email": "keziah@student.richfield.ac.za" }
```

Issues a fresh code. `429` if a code was sent less than 30 seconds ago.

### POST /auth/login

```json
{ "email": "thabo@kotakingdom.co.za", "password": "Password123!" }
```

Returns the same `AuthResponse` shape. Invalid credentials → `400`
"Your email address and password do not match." Signing in before verifying the
e-mail address → `403` "Confirm your email address to finish setting up your account."

### GET /auth/providers

Returns `{ "google": boolean, "apple": boolean }`. A provider is available when both of its
credentials are configured in the server environment.

### POST /auth/oauth/exchange

Completes an OAuth sign-in after the provider redirects back to the web app. The browser's
short-lived sign-in session is exchanged once for the standard `{token, user}` response.

### GET /auth/me

`200` with the user object, or `204` when browsing as a guest.

### POST /auth/password

```json
{ "currentPassword": "Password123!", "newPassword": "NewPassword1" }
```

`204` on success. New password: at least 8 characters and one digit.

---

## Discovery

### GET /discover/home

Optional `lat`, `lng` (the visitor's position). Returns everything the home screen needs:

```json
{
  "zone": { "name": "Richfield Graduate Institute of Technology - Pretoria Campus",
            "address": "291 Helen Joseph Street, Pretoria Central, opposite Sammy Marks Square",
            "anchorLatitude": -25.74635, "anchorLongitude": 28.19255,
            "primaryRadiusKm": 1.0, "extendedRadiusKm": 5.0, "extendedEnabled": false },
  "categories": [ { "id": 1, "name": "Food & Drinks", "slug": "food-drinks",
                    "description": "…", "iconKey": "food", "businessCount": 6 } ],
  "featured": [ { "id": 1, "name": "Kota Kingdom", "rating": 4.7, "reviewCount": 3,
                  "distanceKm": 0.18, "walkingMinutes": 2, "openNow": true, "…": "…" } ],
  "recommended": [ … ],
  "upcomingEvents": [ { "id": 4, "title": "Friday Night Market",
                        "eventDateTime": "2026-10-28T17:00:00", "free": true, "…": "…" } ],
  "stats": { "businesses": 26, "reviews": 45, "categories": 5, "events": 12,
             "averageRating": 4.5, "radiusKm": 1.0 },
  "personalisation": { "personalised": false, "basis": "What's popular near Richfield right now" }
}
```

### GET /businesses

| Parameter | Type | Notes |
|---|---|---|
| `q` | string | Free text across name, description, address, category, amenities. Every token must match. |
| `category` | string | Category slug, e.g. `food-drinks` |
| `price` | int (repeatable) | `1` = R, `2` = RR, `3` = RRR |
| `minRating` | decimal | e.g. `4.5` |
| `maxDistanceKm` | decimal | Walking radius from the reference point |
| `amenity` | long (repeatable) | Amenity ids — a listing must have **all** of them |
| `openNow` | boolean | Uses the structured open/close times |
| `sort` | enum | `recommended` (default), `distance`, `price`, `rating`, `name` |
| `page`, `size` | int | `size` capped at 100 |
| `lat`, `lng` | decimal | Reference point; defaults to the Richfield anchor |

Example — cafés nearest to campus first:

```
GET /api/businesses?category=food-drinks&sort=distance&maxDistanceKm=0.5
```

```json
{
  "items": [
    { "id": 4, "name": "The Daily Grind Café", "categorySlug": "food-drinks",
      "priceLevel": "RR", "priceLabel": "Mid range (R80 - R180)", "rating": 4.0,
      "reviewCount": 3, "distanceKm": 0.05, "walkingMinutes": 1,
      "openNow": true, "thumbnail": "/images/covers/food-drinks-05.svg",
      "amenities": ["Free Wi-Fi", "Power Outlets", "Student Specials"], "status": "ACTIVE" }
  ],
  "total": 6, "page": 0, "size": 24, "totalPages": 1, "sort": "distance",
  "appliedFilters": { "priceLevels": [], "maxDistanceKm": 0.5,
                      "reference": "Richfield campus", "sort": "distance" },
  "zone": { "primaryRadiusKm": 1.0, "extendedEnabled": false, "…": "…" }
}
```

### GET /businesses/map

Coordinates for the zone map: every **active** listing inside the browsing radius,
nearest first. Public.

```json
[{ "id": 17, "name": "Richfield Campus Library", "categoryName": "Study & Social Spaces",
   "iconKey": "study", "latitude": -25.74626017, "longitude": 28.1926996,
   "rating": 4.7, "reviewCount": 3, "distanceKm": 0.02, "walkingMinutes": 1,
   "priceLevel": "R" }]
```

### GET /businesses/{id}

Full profile. Notable fields:

* `rating`, `reviewCount`, `ratingDistribution` (`{"5":2,"4":1,"3":0,"2":0,"1":0}`)
* `distanceKm`, `walkingMinutes`, `directionsUrl` (Google Maps link)
* `images[]`, `amenities[]`, `events[]`, `reviews[]`
* `openNow` (`null` when hours are not supplied → the UI shows "not available" for that field)
* `mine` (you manage this listing), `canReview`, `myReview`
* `status` — non-`ACTIVE` listings are only visible to their owner and administrators

### POST /businesses/{id}/reviews

Requires a bearer token.

```json
{ "rating": 5, "comment": "The R35 student kota is unreal value." }
```

`201` with the saved review. Rules:

* `rating` must be a whole number 1–5 (`400`);
* one review per user per business — posting again **updates** it;
* updates are limited to one every 24 hours (`429` with the hours remaining);
* owners cannot review their own business (`403`);
* the listing must be live (`400`).

### DELETE /businesses/reviews/{reviewId}

`204`. Only the author or an administrator may delete.

### Other read endpoints

| Endpoint | Returns |
|---|---|
| `GET /categories` | Five categories with live listing counts |
| `GET /amenities` | 14 standardised amenities with icon keys |
| `GET /meta/filters` | Categories + amenities + price levels + sorts + zone in one payload |
| `GET /meta/zone` | Just the geographic boundary |
| `GET /meta/price-levels` | `R` / `RR` / `RRR` with weights and labels |
| `GET /events?category&maxDistanceKm&limit` | Upcoming events at live venues, chronological |
| `GET /recommendations?lat&lng&limit` | Personalised ordering (popularity based for guests) |
| `GET /preferences` · `PUT /preferences` | Saved preferences (bearer token) |

---

## Owner portal (role OWNER or ADMIN)

### POST /owner/businesses

```json
{
  "name": "Kota Kingdom", "description": "Quarter loaf, polony, cheese, chips, atchar.",
  "address": "268 Pretorius Street, Pretoria Central",
  "latitude": -25.745991, "longitude": 28.194345,
  "priceLevel": "R", "categoryId": 1,
  "operatingHours": "Mon-Sat 06:30 - 20:00", "openTime": "06:30", "closeTime": "20:00",
  "phone": "072 445 1180", "whatsapp": "072 445 1180", "website": null,
  "amenityIds": [6, 8, 13], "imageUrls": ["/images/covers/food-drinks-01.svg"],
  "reactivate": true
}
```

`201` with the full profile, `status: "PENDING"`. Validation:

* coordinates are converted into a zone classification; outside 5 km → `400` with the distance;
* `openTime`/`closeTime` must both be supplied or both omitted, and opening must precede closing;
* the first image URL becomes the primary thumbnail; at most 8 images are stored.

### PUT /owner/businesses/{id}

Same body. A rejected listing re-enters `PENDING`; `reactivate: true` re-submits after a soft hide.

### DELETE /owner/businesses/{id}

`204`. Cascades to images, events and reviews.

### Events

| Method | Endpoint | Body |
|---|---|---|
| GET | `/owner/businesses/{id}/events` | – |
| POST | `/owner/businesses/{id}/events` | `{ "title": "Live acoustic", "description": "…", "venue": "Deck", "eventDateTime": "2026-11-08T19:00:00", "ticketPrice": 0 }` |
| DELETE | `/owner/events/{eventId}` | – |

`eventDateTime` must be in the future; `ticketPrice` may be `0` for free events.

---

## Administrator (role ADMIN)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/admin/businesses?status=PENDING` | Queue for verification |
| POST | `/admin/businesses/{id}/approve` | Body optional: `{ "note": "Verified in person." }` |
| POST | `/admin/businesses/{id}/reject` | Body optional: `{ "note": "Please add hours." }` |
| GET | `/admin/stats` | Totals by status and category, review count, average rating |

---

## Reports (role ADMIN)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/admin/reports` | Catalogue of the five reports |
| GET | `/admin/reports/{key}` | One report as a table |
| GET | `/admin/reports/{key}/csv` | CSV download (`text/csv` with `Content-Disposition`) |

Keys: `listings-by-category`, `verification-audit`, `review-activity`,
`owner-performance`, `events-schedule`.

```json
{
  "key": "listings-by-category",
  "title": "Listings by category",
  "description": "Verified, pending and rejected listings in the 1 km pilot zone, per category.",
  "generatedAt": "2026-10-27 21:06",
  "columns": ["Category", "Live", "Pending", "Rejected", "Total", "Average rating", "Reviews"],
  "rows": [
    ["Food & Drinks", "6", "1", "0", "7", "4.4", "10"],
    ["TOTAL", "26", "2", "1", "29", "4.4", "45"]
  ],
  "metrics": [
    { "label": "Live listings", "value": "26" },
    { "label": "Awaiting review", "value": "2" },
    { "label": "Categories", "value": "5" },
    { "label": "Average rating", "value": "4.4" }
  ]
}
```

CSV output (RFC 4180, every cell quoted, CRLF line endings):

```csv
"Category","Live","Pending","Rejected","Total","Average rating","Reviews"
"Food & Drinks","6","1","0","7","4.4","10"
```

## System backup (role ADMIN)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/admin/backup/summary` | Row counts per table |
| GET | `/admin/backup` | Full JSON backup as a download |
| POST | `/admin/backup/restore` | Restore; body is the raw backup JSON |

```bash
# download
curl -s localhost:8080/api/admin/backup -H "Authorization: Bearer $TOKEN" -o backup.json

# restore
curl -s -X POST localhost:8080/api/admin/backup/restore \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data-binary @backup.json
```

Restore response:

```json
{ "restoredAt": "2026-10-27T21:05:23+02:00[Africa/Johannesburg]",
  "counts": { "categories": 5, "amenities": 14, "users": 17, "businesses": 29,
              "images": 56, "events": 12, "reviews": 45, "preferences": 0 },
  "message": "Backup restored: 29 listings, 45 reviews and 17 accounts." }
```

Restore runs in one transaction and rejects a file whose `format` is not
`cbdventures-backup`, or whose `version` is newer than the server supports (400).

---

## Operations

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Liveness/readiness probe |
| `GET /actuator/info` | Build metadata |
| `GET /h2-console` | Database console (demo profile only) |

---

## cURL quick start

```bash
# 1. Sign in as an owner
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"thabo@kotakingdom.co.za","password":"Password123!"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')

# 2. See your listings
curl -s localhost:8080/api/owner/businesses -H "Authorization: Bearer $TOKEN"

# 3. Search the zone: cheapest first, must have Wi-Fi, open now
curl -s "localhost:8080/api/businesses?price=1&amenity=1&openNow=true&sort=price"

# 4. Leave a review (as a student)
STUDENT=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"keziah@student.richfield.ac.za","password":"Password123!"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')
curl -s -X POST localhost:8080/api/businesses/1/reviews \
  -H "Authorization: Bearer $STUDENT" -H 'Content-Type: application/json' \
  -d '{"rating":5,"comment":"Best kotas near campus."}'
```
