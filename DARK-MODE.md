# CBDVentures — Dark Mode Edition

The final hand-off update adds configurable Google/Apple OAuth, an interactive OpenStreetMap with
pins from the business map API, and working event saves on the current device. OAuth needs provider
credentials in the deployment environment; map tiles need internet access.

**CBD Ventures — Pretoria CBD in Your Pocket**
IT Project · Group 5 · Supervisor: Ma'am Ntokozo
Second build of the same application, restyled to the five reference screens supplied for the dark-mode brief.

> The light build is untouched and still submits exactly as before (`/home/user/cbdventures`, ZIP `CBDVentures-Group5-Final.zip`).
> This folder is the **same application** — same domain model, same database schema, same REST API, same automated test suite (both now 76/76 and 74/74 respectively) — with a different interface layer. Nothing was forked: `/api/**`, `db/schema_mysql.sql` and the service layer are the light build plus email verification and one new read-only endpoint, both documented below.

---

## 1. How the two versions relate

| | Light mode | Dark mode |
|---|---|---|
| Folder | `cbdventures/` | `cbdventures-dark/` |
| Interface | Portal shell: fixed side rail, cards on cream | Mobile-first app shell: 520 px column, top bar, bottom tab bar |
| Palette | Cream `#FBF9F6`, violet `#8B5CF6`, amber `#F5A623` | Near-black `#0B0B0D`, surfaces `#17181C`, orange `#CE6338`, gold `#E0A33B` |
| Screens | dashboard, listings, reports, backup | welcome → sign-in → sign-up → verify → discover → explore → listing → map → **Ask (assistant)** → events → profile (+ owner, admin, reports, backup) |
| Backend | Spring Boot 4.1.2 · Java 25 · H2/MySQL | identical |
| Tests | 74/74 | **86/86** (`ApiHttpTest` 14, `AssistantHttpTest` 9, `DiscoveryIntegrationTest` 18, `OpeningHoursTest` 14, `ReportingTest` 12, `GeoUtilsTest` 7, `ZoneServiceTest` 6, `ScoringTest` 6) |

Both share `data/cbdventures.mv.db` **by name**, so run one at a time (dark mode in this workspace is served on **:8081** while it is being marked). Everything else — accounts, listings, reviews, events, reports, CSV exports, JSON backup/restore — behaves identically because it is the same code.

---

## 2. Screens implemented against the reference images

| Reference screenshot | Screen in the build | Route |
|---|---|---|
| Splash with Union Buildings hero, orange primary + outlined secondary | Welcome | `#/welcome` |
| "Welcome Back", icon fields, eye toggle, Forgot Password, Google/Apple | Sign in | `#/signin` |
| "Create Profile", circular camera avatar + UPLOAD PHOTO, terms checkbox | Sign up | `#/signup` |
| "Verify Your Email", 6 orange-bordered digit boxes, resend | Verify | `#/verify?email=…` |
| Logged-in home, greeting, search pill, category chips, featured + popular rails, dark bottom nav | Discover | `#/discover` (also `#/`) |

Beyond the five references, the rest of the application was carried into the same visual language: **Explore** (search, sort chips, bottom-sheet filters), **Listing detail** (hero, hours, amenities, mini-map, rating distribution, review modal), **Map** (1 km zone with every live pin), **Events**, **Profile**, and the reused **Owner**, **Admin verification queue**, **Reports** and **Backup** modules.

### Deliberate deviations from the screenshots
1. **Bottom navigation** shows Discover · Explore · Map · **Ask** · Events · Profile. The reference's
   *Chat* slot is now the **Ask** tab — the CBDVentures student assistant, ported into this build
   (see section 4). The reference's *Pay* slot stays out: there is no payment model in the database,
   and a dead tab would be a worse demo than an honest omission.
2. **Wordmark** stays "CBDVentures" (the product name in the brief) rather than the reference's "Tshwane Explorer"; "Discover Pretoria Central" is kept as the tagline.
3. **Social sign-in** (Google/Apple) uses Spring Security OAuth when provider credentials are set in the deployment environment. If credentials are missing, the buttons explain which Render variables are required.
4. **Photo upload** on Create Profile previews locally in the circle; there is no file storage in the schema, so the avatar is not persisted to the account.
5. **reCAPTCHA** in the reference is replaced by the real 6-digit e-mail verification that the backend implements ("Protected by one-time email verification").

---

## 3. What changed in the backend

The dark build adds **email verification** to registration, configurable Google/Apple OAuth, and a read-only endpoint for live map pins.

* `POST /api/auth/register` → `201 {email, fullName, verificationRequired:true, demoCode, expiresInMinutes:10}`
* `POST /api/auth/verify {email, code}` → `{token, user{…, emailVerified:true}}`
* `POST /api/auth/resend {email}` → new code; `429` if requested within 30 s
* `GET /api/auth/providers` → shows which configured OAuth providers are available
* `POST /api/auth/oauth/exchange` → exchanges the provider sign-in session for the app JWT
* Signing in before verifying → `403 "Confirm your email address to finish setting up your account."`
* Codes: six digits, 10-minute TTL, five attempts, held in memory (a demo needs no SMTP); `cbd.verification.expose-code=true` returns the code as `demoCode` so the marker can see the flow without an inbox.
* `GET /api/businesses/map` → `[{id, name, categoryName, iconKey, latitude, longitude, rating, reviewCount, distanceKm, walkingMinutes, priceLevel}]` for every active listing inside the browsing radius, nearest first. This is what draws the pins — the client never guesses coordinates.

Files touched: `domain/User.java`, `web/dto/AuthDtos.java`, `service/AuthService.java`, `service/EmailVerificationService.java`, `web/AuthController.java`, `config/SecurityConfig.java`, `service/BusinessService.java`, `web/BusinessController.java`, `web/dto/MapPin.java`, `src/test/…/ApiHttpTest.java`.

---

---

## 4. The student assistant (Ask tab)

The team's standalone assistant project (`cbdventures-assistant`, a Node HTTP server) was **ported
into this Spring Boot build** rather than embedded as a second service. One jar, one port, no Node
process and no API key:

| | Standalone project | In this build |
|---|---|---|
| Runtime | Node 20, `node server.js` | Spring Boot 4.1.2, `java -jar` |
| Port | 3000 (a separate service) | the app's own port (8080 / 8081) |
| Data | `data/places.json` | `src/main/resources/assistant/places.json` |
| Photos | `public/images/*.jpg` | `src/main/resources/static/images/places/*.jpg` |
| Look | navy `#122941` chat app | the app's own dark shell: near-black surfaces, `#CE6338` accent, 520 px column, six-tab bar |
| Reply engine | rule-based local engine + optional paid AI provider | the same rule-based engine, fully offline (no server-side API key to leak) |

### 4.1 What was kept, and what changed

* **Kept** — the curated 19-place student directory with its aliases and verification notes; the 1 km
  Richfield radius rule (a place is only "nearby" when its stored coordinates fall inside it); typo
  tolerant name matching; greeting / help / thanks handling; the nine-category keyword engine; the
  student budget planner; the honest "I don't know" answers for opening hours, menus, weather and
  off-topic places; and the verification notes shown on student-submitted listings.
* **Changed** — the interface is a screen inside this app (route `#/assistant`, sixth tab **Ask**)
  built from the existing tokens, cards, chips and toasts; the API is served under `/api/assistant`;
  `?v=7` cache-busting, the navy palette and the old class names are gone; the transcript lives in
  memory for the session; reply lists are capped at eight places so a long answer stays readable.

### 4.2 API (public — no token needed, so a guest can use the guide)

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/assistant/config` | campus pin, 1 km radius, the nine categories, place counts |
| `GET` | `/api/assistant/places?category=&q=` | the curated directory, filterable |
| `GET` | `/api/assistant/places/{id}` | one place (404 when unknown) |
| `POST` | `/api/assistant/chat` | `{messages:[{role,content}]}` → `{reply, places[], quickReplies[], mode}` |

Guard rails carried over from the original, plus two of ours: 30 chat requests per minute per address
(held in memory), a 2 000-character message cap, at most 30 messages of history, the last message
must come from the user, and an unreadable body answers **400** — never a 500.

### 4.3 Tests and screenshots

`AssistantHttpTest` (9 tests) proves the tab is reachable without a token, the 1 km rule still holds,
category filtering works, a food question returns place cards, a typo ("asain delight") still finds
Asian Delight Shawarma, the R50 answer refuses to invent prices, "eat on Mars" is declined, an empty
message is rejected with 400 and an unknown place is a 404. The dark suite is now **86/86**.

Screenshots: `screenshots/assistant/` — 01 tab, 02 reply, 03 place cards, 04 directory filter,
05 desktop, 06 Discover with the six-tab bar.

### 4.4 Email verification in the demo

Registration already ends on a verification screen: `/api/auth/register` creates the account
unverified and issues a six-digit code (10-minute life, five wrong attempts burn it, one live code per
address, 30-second resend cooldown), `/api/auth/login` answers **403 "Confirm your email address"**
until the code is entered, and `/api/auth/verify` then returns the session token. The demo transport
writes the code to the server log and, only while `cbd.verification.expose-code=true` (the demo
default), returns it to the client so the verify screen can be marked. Set it to `false` for a
production-style build.

---

## 5. Running it

```bash
cd cbdventures-dark
./mvnw -B test                 # 75/75
./mvnw spring-boot:run         # http://localhost:8080
```

Demo accounts (seeded on first run):

| Role | E-mail | Password |
|---|---|---|
| Administrator | `admin@cbdventures.co.za` | `Admin123!` |
| Business owner | `thabo@kotakingdom.co.za` | `Password123!` |
| Visitor | `keziah@student.richfield.ac.za` | `Password123!` |

Seeded data: 5 categories, 14 amenities, 29 listings (26 live / 2 pending / 1 rejected), 203 hours rows, 45 reviews, 12 events, 17 accounts.

---

## 6. Ten-minute marking walkthrough

1. Open `/` → **Welcome** (hero, Sign In, Create Account).
2. **Create Account**: fill the form, tick the terms box → lands on **Verify Your Email**. The six boxes are filled from the code shown in the “demo code” notice (or from the server log).
3. Click **Verify** → signed straight in; the greeting is now "Hello, Explorer!".
4. **Discover**: search pill, category chips, Featured rail, Popular cards, Happening soon.
5. **Explore**: search, sort chips, filter sheet (category, price R/RR/RRR, distance, rating, amenities, open-now, near-me) → results update.
6. Open a listing: hours, amenities, mini-map, rating distribution, photo, directions, tel link, review modal (one review per person; edits once per 24 h).
7. **Map**: 26 pins inside the 1 km ring, ordered by walking distance.
8. **Ask** (the assistant): ask *"I'm hungry, where can I eat?"* → the reply lists nearby food places
   and drops place cards under the composer; tap **Ask about it** on one, then *"what can I buy with
   R50?"* → the budget plan, which explicitly says the amounts are examples, not confirmed prices.
   Use the **Browse the guide** chips to filter by category. Unverified student-submitted places show
   a gold badge instead of a made-up distance.
9. **Events**: This week / This month / Free entry filters.
9. Sign out → sign in as the **owner** → Owner dashboard: add/edit a listing, publish events.
10. Sign in as the **admin** → verification queue (approve/reject), five reports with CSV download, JSON backup + restore.

---

## 7. Verification performed

* `./mvnw -B test` → **86 tests, 0 failures, 0 errors** (light build: 74/74 after the same hardening fix; the dark build is ahead by email verification and the assistant).
* Every screen loaded in headless Chromium at 390×844 and 1280×900 with **zero console errors and zero page errors** (scripts in `verify-dark/`).
* Functional checks run through the UI: registration → verification → signed-in home, filter sheet apply, listing review modal (pre-filled edit state), owner "Add a listing" form with live zone feedback, profile menu, admin queue, reports, backup, toast system.
* Bugs found and fixed during this pass, all in the dark layer:
  1. `toast`/`openModal` were writing to `#toast-stack` / `#modal-root` while `index.html` declared `#toast-host` / `#modal-host` — every dialog and toast was silently dropped.
  2. An open dialog survived route changes (a filter sheet was still layered over the Map screen) — the router now closes any open modal on navigation.
  3. Events screen threw `list.querySelectorAll is not a function` (a local `list` shadowed the results node).
  4. A visitor who already had a review could not edit it (`canReview` is false when `myReview` exists) — the review button now checks for an existing review first.
  5. Reports table forced the whole page wider than the phone (`grid` child with auto min-width) — `minmax(0,1fr)` + `min-width:0` on panels and scroll containers; report chips restyled to the app's pill language.
  6. The old schematic zone map was replaced with an interactive OpenStreetMap map using the API's live listing coordinates.
  7. Avatars rendered as ellipses in the profile header (flex-shrunk circles) — pinned with `aspect-ratio` and `flex: 0 0 auto`.
  8. The offline banner ignored its `hidden` attribute and overlapped the top bar; it now toggles with `navigator.onLine` and pushes the app down when shown.
  9. The desktop welcome illustration filled the viewport; capped at 360 px from 720 px up.
 10. `GET /api/businesses/not-a-number` answered **500** (type mismatch reached the catch-all). Fixed in **both** builds: a `MethodArgumentTypeMismatchException` handler now returns a clean `400` with a friendly message, covered by a new test in each suite.

---

## 8. Screenshots

`verify-dark/` holds the captured run (mobile unless noted):
`01-welcome`, `02-signin`, `03-signup`, `04-verify`, `05-discover-guest`, `06-explore`, `06b-filters`, `07-business`, `08-map`, `09-events`, `10-discover-signedin`, `11-profile-visitor`, `12-profile-admin`, `13-admin`, `14-reports`, `15-backup`, `16-owner`, `17-owner-form`, `18-review-modal`, `19-profile-visitor-toast`, `20-desktop-discover`, `21-desktop-reports`, and the assistant set
`assistant/01-assistant-tab` … `assistant/06-discover-with-six-tabs`.
