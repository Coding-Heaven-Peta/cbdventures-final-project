# 4.7 Interface Design

This section documents the menu, input and output design of **CBDVentures**. It describes the two
delivered builds of the same application:

* **Light mode (v1)** — the portal layout described in Section 4.4.4.
* **Dark mode (v2)** — a mobile-first build of the same system, built to the five supplied reference
  screens. It uses identical navigation logic, forms, validation and outputs; only the visual shell
  differs.

Screenshot references below point at the evidence folders: `screenshots/` (light build) and
`docs/screenshots-dark/` (dark build).

---

## 4.7.1 Menu Interface Design

### 4.7.1.1 Navigation principles

Six rules govern the menu system:

1. **One flat primary menu, six items maximum.** The bottom tab bar (dark) carries Discover,
   Explore, Map, Ask, Events and Profile; the light build's top navigation carries the same
   destinations minus Ask. Anything deeper is reached from a screen, never from a growing menu.
   Ask is the assistant tab from the reference mock-up's "Chat" slot — it is a single screen with
   one input, so it earns a tab rather than a sub-menu.
2. **The tab bar never disappears inside the app.** It stays fixed on every logged-in screen, so the
   user is never more than one tap from the start. Only the authentication screens run "chromeless"
   (no tabs), because they are a linear task the user must finish or leave.
3. **The menu reflects the signed-in role.** Items the current role cannot use are not shown — an
   owner never sees an Admin item, and a guest never sees a Profile item. Access is *also* enforced
   server-side (see 4.7.2), so hiding the item is convenience, not security.
4. **Destructive or interruptive tasks use a modal, not a new screen.** Filters, the review form and
   the sign-out confirmation overlay the current page so the user does not lose their place.
5. **Sign-in is a screen, not a modal.** Account creation, sign-in and e-mail verification are full
   screens with a back arrow, so they work as deep links and survive a page refresh.
6. **Every screen has a stable URL.** Navigation uses hash routes (`#/explore`), so the back button,
   bookmarking and refresh all behave as users expect.

### 4.7.1.2 Menu hierarchy

```
CBDVentures
│
├── (auth — chromeless, no menu)
│   ├── Welcome ........................ #/welcome
│   ├── Sign In ........................ #/signin
│   ├── Create Profile ................. #/signup
│   └── Verify Your Email .............. #/verify?email=…
│
└── (app — persistent tab bar)
    ├── [1] Discover ................... #/            ← landing screen
    │     ├── Search pill ─────────────► Explore
    │     ├── Category chip ───────────► Explore (filtered)
    │     ├── Featured rail ───────────► Business profile
    │     └── "See All" ───────────────► Explore
    ├── [2] Explore .................... #/explore
    │     ├── Sort chips (in place)
    │     ├── Filter button ───────────► Filter sheet (modal)
    │     └── Result card ─────────────► Business profile  #/business/{id}
    ├── [3] Map ........................ #/map
    │     └── Pin / list row ──────────► Business profile
    ├── [4] Ask (student assistant) .... #/assistant
    │     ├── Quick-reply chips ───────► sends a question to the guide
    │     ├── Place card → Directions ─► Google Maps walking route from campus
    │     ├── Place card → Ask about it► asks the guide about that place
    │     └── Browse the guide chips ──► filters the curated directory
    ├── [5] Events ..................... #/events
    │     ├── Window chips (All / This week / This month / Free)
    │     └── "See the venue" ─────────► Business profile
    └── [6] Profile .................... #/profile
          ├── Browse the zone ─────────► Explore
          ├── Your listings ───────────► Owner dashboard   #/owner      (OWNER only)
          ├── List your business ──────► Sign up           (guest/visitor)
          ├── Verification queue ──────► Admin queue       #/admin     (ADMIN only)
          ├── Reports ─────────────────► Reports           #/reports   (ADMIN only)
          ├── System backup ───────────► Backup            #/backup    (ADMIN only)
          ├── Change password ─────────► Confirm dialog (modal)
          ├── Clear offline cache ─────► Toast confirmation
          └── Sign out ────────────────► Confirm dialog (modal) → Welcome
```

### 4.7.1.3 Screen inventory and mapping to Section 4.4.4

Section 4.4.4 proposed six Android screens and four web pages. The implemented system covers all of
them; the table records where each one now lives.

| Section 4.4.4 item | Implemented as | Route / file | Build |
|---|---|---|---|
| Android **Home** | Discover (greeting, search, categories, featured + popular rails) | `#/` | both |
| Android **Results** | Explore (search, sort chips, filter sheet, result cards) | `#/explore` | both |
| Android **Profile** | Business profile (hours, amenities, map, rating, reviews) | `#/business/{id}` | both |
| Android **Review** | Review form (star picker + comment) over the profile | modal | both |
| Android **Owner Login** | Sign In (owner role) — same screen, role determined by the account | `#/signin` | both |
| Android **Manage Listing** | Owner dashboard + Add/Edit listing + Manage events | `#/owner` | both |
| Web **index** | Discover / home | `index.html` → `#/` | light |
| Web **category** | Explore pre-filtered by category (deep link) | `#/explore?category=food-drinks` | both |
| Web **profile** | Business profile | `#/business/{id}` | both |
| Web **owner** | Owner dashboard | `#/owner` | both |
| *(added)* **Map** | Zone map with all live pins | `#/map` | dark |
| *(added)* **Events** | What's on, with time-window filters | `#/events` | both |
| *(added)* **Assistant (Ask)** | Student guide: chat, suggested places, curated directory | `#/assistant` | dark |
| *(added)* **Admin verification** | Queue with approve/reject | `#/admin` | both |
| *(added)* **Reports** | Five management reports + CSV | `#/reports` | both |
| *(added)* **Backup** | Download / restore the database | `#/backup` | both |
| *(added)* **About** | How it works, for the marker | `#/about` | light |

### 4.7.1.4 Menu visibility by role

| Menu item | Guest | Visitor | Owner | Administrator |
|---|:--:|:--:|:--:|:--:|
| Discover, Explore, Map, Ask, Events | ✔ | ✔ | ✔ | ✔ |
| Business profile + read reviews | ✔ | ✔ | ✔ | ✔ |
| Leave / edit a review | ✕ (prompted to sign in) | ✔ | ✕ (own listing) | ✕ |
| Profile (account) | ✕ | ✔ | ✔ | ✔ |
| Owner dashboard | ✕ | ✕ | ✔ | ✕ |
| Verification queue | ✕ | ✕ | ✕ | ✔ |
| Reports + CSV | ✕ | ✕ | ✕ | ✔ |
| System backup / restore | ✕ | ✕ | ✕ | ✔ |
| Change password / sign out | ✕ | ✔ | ✔ | ✔ |

**Behaviour when a menu item is reached without permission**

| Situation | System response |
|---|---|
| Guest opens `#/profile` | Redirected to `#/signin`, with the intended destination remembered |
| Visitor opens `#/admin` | "That area is for administrators" screen — "Your account is signed in, but it does not have access to this module." + *Back to discover* button |
| Owner opens `#/owner` while signed out | Sign-in screen; on success the owner dashboard loads |
| Token expires mid-session | First 401 response signs the user out and returns them to Sign In with a toast |
| Unknown route | Not-found screen with a link to Discover (never a blank page) |

### 4.7.1.5 Navigation behaviour

| Behaviour | Implementation |
|---|---|
| Landing screen | First-ever visit opens **Welcome**; afterwards Discover. Tracked with `localStorage['cbd.welcomeSeen']` |
| Back navigation | Browser/virtual back returns to the previous hash route; modal back closes the modal only |
| Deep links | Every screen is addressable, including filtered results (`#/explore?category=salons&price=1`) |
| Active state | The current tab is highlighted; sub-routes mark their parent (business → Explore, admin/reports/backup → Profile) |
| Screen transition | Modal overlay (filters/review/confirm) vs full route change (auth screens, dashboards) |
| Offline | Menu stays usable; cached listings remain browsable and an offline banner appears |

**Figures:** `01-welcome`, `05-discover-guest`, `06-explore`, `06b-filters`, `08-map`, `09-events`,
`11-profile-visitor`, `12-profile-admin` (dark); `01-home`, `02-explore-filters`, `04-events`,
`08-owner-dashboard` (light).

---

## 4.7.2 Input Design

### 4.7.2.1 Input design standards

| Standard | Application in CBDVentures |
|---|---|
| Label placement | Label above the field, uppercase and small in the dark build; never placeholder-only |
| Required fields | Marked with `*` in owner/admin forms; auth screens mark the whole form as required |
| Field affordances | Leading icons (person, envelope, padlock) show the expected data type at a glance |
| Password entry | Masked by default with an eye toggle; `autocomplete` set correctly for password managers |
| Keyboard types | `type="email"`, `inputmode="email"` and `type="search"` so mobile keyboards match the data |
| Inline validation | Errors appear directly below the form, above the submit button, in plain English |
| Submit feedback | Button disables and changes text (`Signing in…`, `Creating…`, `Posting…`) to prevent double submission |
| Server authority | Client checks give instant feedback; the server re-validates everything and its message replaces the client's |
| Destructive actions | Require an explicit confirmation dialog (sign out, restore backup, delete listing) |
| No dark patterns | No pre-ticked consent boxes, no hidden required fields, no fake social login |

### 4.7.2.2 Registration form — Create Profile (`#/signup`)

| # | Field | Control | Required | Validation rule | Error message |
|---|---|---|---|---|---|
| 1 | Profile photo | Circular file picker (`accept="image/*"`), local preview only | No | Image file; stored on device only (POPIA minimisation) | — |
| 2 | Full name | Text, `autocomplete="name"`, max 100 | Yes | ≥ 2 characters | "Tell us your name so owners know who is reviewing." |
| 3 | Email address | Email, `inputmode="email"` | Yes | Valid pattern; unique in `app_user` | "That email address does not look right." / "An account with this email address already exists. Try signing in instead." |
| 4 | Password | Password + eye toggle, max 100 | Yes | ≥ 8 characters, contains a number | "Use at least 8 characters for your password." / "Password: must be at least 8 characters" / "must contain at least one number" |
| 5 | Confirm password | Password + eye toggle | Yes | Must equal field 4 | "The two passwords do not match." |
| 6 | I am joining as | Select: *A student, local or visitor* / *A business owner (list my business)* | Yes | `VISITOR` or `OWNER` only | "Administrator accounts are created by the platform team." |
| 7 | Terms & Privacy | Checkbox | Yes | Must be ticked | "Please accept the terms to create an account." |

On success the user is taken to **Verify Your Email**; no session exists yet.

### 4.7.2.3 Sign-in form — Welcome Back (`#/signin`)

| # | Field | Control | Required | Validation rule | Error message |
|---|---|---|---|---|---|
| 1 | Email address | Email + leading envelope icon | Yes | Must exist | "That email address is not registered." |
| 2 | Password | Password + eye toggle | Yes | Must match BCrypt hash | "That email address and password do not match." |
| 3 | *(account not verified)* | — | — | `emailVerified = false` blocks sign-in | "Confirm your email address to finish setting up your account." → route to `#/verify` |
| 4 | Forgot Password? | Link → modal | — | Demo has no SMTP; explains the reset path | Informational only |
| 5 | Google / Apple | Buttons | — | OAuth provider credentials must be configured by deployment | Starts provider sign-in or explains missing Render variables |
| 6 | Demo accounts | Three chips (User / Owner / Admin) | — | Fills the form for marking | — |

### 4.7.2.4 Email verification (`#/verify?email=…`)

| # | Field | Control | Required | Validation rule | Error message |
|---|---|---|---|---|---|
| 1 | Verification code | Six single-digit boxes, auto-advance, paste-aware, backspace moves back | Yes | Exactly 6 digits; 10-minute TTL; 5 attempts | "That code is not right." / "That code has expired. Tap “Resend code” to get a new one." / "Too many incorrect attempts. Tap “Resend code” to start again." / "Enter the six-digit code from the email." |
| 2 | Resend Code | Link | — | Minimum 30 s between sends | HTTP 429 → "Please wait 27 seconds before requesting another code." |

### 4.7.2.5 Search input

| Element | Control | Behaviour | Failure handling |
|---|---|---|---|
| Search pill | Single text input, `type="search"` | Debounced 320 ms; searches name, description, address, category and amenity | Empty result → "No matches inside the zone" + a prompt to widen the filters |
| Placeholder | "Search listings…" (Explore) / "Search monuments, eats, hotels…" (Discover) | Sets expectation of what is searchable | — |
| Category chips | Horizontal scrolling pills, single-select | Filters the result set; "All" clears | — |
| Sort chips | Best match · Closest · Lowest price · Highest rated | Re-queries; current sort highlighted | — |

### 4.7.2.6 Filter sheet (modal, `#/explore` → Filters)

| # | Group | Control | Options | Rule |
|---|---|---|---|---|
| 1 | Category | Single-select chips | All · Food & Drinks · Restaurants & Sit-Down Eateries · Salons & Personal Care · Study & Social Spaces · Events & Entertainment | Zero or one |
| 2 | Price range | Multi-select chips | R (under R80) · RR (R80–R180) · RRR (R180+) — sent to the API as `1`, `2`, `3` | Any number |
| 3 | Walking distance | Single-select chips | 250 m · 500 m · 1 km | Zero or one |
| 4 | Minimum rating | Single-select chips | 3★ and up · 4★ and up · 4.5★ and up | Zero or one |
| 5 | Amenities | Multi-select chips | Air Conditioning · Backup Power · Quiet Atmosphere · Power Outlets · Free Wi-Fi … | Any number |
| 6 | Open now | Checkbox | — | Boolean |
| 7 | Use my location | Button | Requests geolocation; recalculates distances from the device | Denied → "Location permission was declined. Showing distances from the Richfield campus." |
| 8 | Reset | Button | Clears the draft (not yet applied) | — |
| 9 | Show results | Primary button | Applies the draft and returns to the list | — |

Draft changes are **not** applied until *Show results* is pressed, so an accidental tap cannot wipe a
user's result set.

### 4.7.2.7 Review form (modal, on a business profile)

| # | Field | Control | Required | Validation rule | Error message |
|---|---|---|---|---|---|
| 1 | Star rating | Five-star picker, `role="radiogroup"` | Yes | Integer 1–5 | "Choose a star rating first." / "Please choose a star rating between 1 and 5." |
| 2 | Your review | Textarea, max 1000 | No | Length cap | "Comment: must be 1000 characters or fewer" |
| 3 | Eligibility | — | — | One review per user per business; owners and admins excluded; edit at most once per 24 h | "Reviews open once this listing is live." / server rule messages |

### 4.7.2.8 Listing management form (modal, `#/owner` → Add listing)

| # | Field | Control | Required | Validation rule |
|---|---|---|---|---|
| 1 | Business name * | Text, max 120 | Yes | 2–120 characters |
| 2 | Category * | Select (5 seeded categories) | Yes | "Please choose one of the five supported categories." |
| 3 | Street address * | Text, max 255 | Yes | Non-blank |
| 4 | Latitude * / Longitude * | Numeric | Yes | −90…90 / −180…180 → "must be a valid latitude" / "must be a valid longitude" |
| 5 | Live zone feedback | Read-only panel | — | Recalculates on change: "Inside the 1 km pilot zone (0 m from Richfield). This listing can publish after review." or the measured distance outside it |
| 6 | Description | Textarea, max 2000 | No | — |
| 7 | Price range * | Select R / RR / RRR | Yes | — |
| 8 | Hours summary | Text, max 100 | No | e.g. "Mon–Sat 06:30–20:00" |
| 9 | Default opening / closing time | Time inputs | No | Both or neither; opening before closing |
| 10 | Phone / WhatsApp / Website | Text | No | Format checked on save |
| 11 | Amenities | Multi-select chips (14 seeded) | No | — |
| 12 | Photos | One URL per line, max 8 | No | Valid image URLs |

**Hours and price messages (server + database):** "Please provide both the opening and closing time,
or leave both blank." · "The opening time must be earlier than the closing time." · "Please give both
an opening and a closing time for Monday." · "must use the 24-hour HH:mm format, e.g. 07:30." ·
"Every opening-hours row must name a day of the week (1 = Monday to 7 = Sunday)." ·
"must be 1 (Monday) to 7 (Sunday)" · "cannot contain more than 7 days."

Result: the listing is saved as **PENDING**, not public, and appears in the admin queue.

### 4.7.2.9 Event form (modal, owner → Manage events)

| # | Field | Control | Required | Validation rule / message |
|---|---|---|---|---|
| 1 | Title * | Text, max 120 | Yes | Non-blank |
| 2 | Description | Textarea | No | — |
| 3 | Date & time * | `datetime-local` | Yes | "The event must be scheduled for a future date and time." / "must be a future date and time" |
| 4 | Ticket price (R) | Number, min 0 | No | "The ticket price cannot be negative. Use 0 for a free event."; 0 = Free badge |
| 5 | Room / area in venue | Text | No | — |
| 6 | Venue | Resolved from the owning listing | Yes | "You can only add events to your own listings." |

### 4.7.2.10 Administrative and system inputs

| Screen | Input | Validation / confirmation |
|---|---|---|
| Verification queue | Approve / Reject, with an administrator note | Note shown to the owner on rejection; "Unsupported verification decision." guards bad values; "Only administrators can verify listings." |
| Reports | Report tab, Download CSV, Print | Read-only; "That report does not exist." for an unknown key |
| Backup — restore | File picker (`application/json`) + confirm dialog | "That file is not a CBDVentures backup (wrong format marker)." · "The backup file is empty." · "The backup contains no categories, so it cannot be restored." · "This backup was created by a newer version of CBDVentures." · "Only administrators can restore a system backup." |
| Profile — change password | Current password, new password, confirm | ≥ 8 characters with a number; current password verified |
| Sign out / delete listing | Confirmation dialog | Explicit confirm, destructive button styled red |

### 4.7.2.11 How validation messages reach the user

Every failure, from any layer, ends up in the same place — one message under the form:

| Layer | Mechanism | Example |
|---|---|---|
| Client (instant) | Field checks in the view before the request | "The two passwords do not match." |
| API contract | Bean Validation on the DTO, formatted as *Field: message* | "Email: must be a valid email address" |
| Business rules | Service exceptions with plain-English text | "That email address and password do not match." |
| Database | CHECK constraints and 8 triggers, so direct SQL cannot bypass a rule | A listing 3 km from campus is refused at the database |
| Unexpected | `GlobalExceptionHandler` catch-all | "Something went wrong on our side. Please try again in a moment." |

---

## 4.7.3 Output Design

### 4.7.3.1 Output standards

1. **Card-based results.** Every listing is one card with the same field order everywhere (image →
   name → category → rating → distance → price), so users learn to read it once.
2. **Colour is never the only signal.** Open/closed, free/paid and errors carry text and an icon, not
   just green/red.
3. **Four states for every data screen.** Loading (skeleton), success (content), empty (explanation +
   next action), error (plain message + retry) — no blank screens.
4. **Numbers are formatted, not raw.** `20 m · 1 min walk`, `R`, `RR`, `4.7` with one decimal,
   `Friday 30 Oct, 17:00`, `2 days ago`.
5. **Reports carry their own context.** Title, description, generation timestamp, row count, and a
   totals row.

### 4.7.3.2 Search results output (`#/explore`)

```
┌──────────────────────────────────────────────┐
│  Find it in the zone                     (⚙) │  ← title + filter button
│  Explore                                     │
├──────────────────────────────────────────────┤
│  🔍  Search listings…                        │
│  [Best match][Closest][Lowest price] →       │  ← sort chips scroll
│  26 listings · sorted by best match          │  ← result count
├──────────────────────────────────────────────┤
│  ┌───────┐  Richfield Campus Library   ☆ 4.7  │
│  │ image │  📍 20 m · 1 min walk   R          │  ← distance + price
│  └───────┘  🕘 07:30 - 21:00    [CLOSED]     │  ← today's hours + state
├──────────────────────────────────────────────┤
│  ┌───────┐  Kota Kingdom               ☆ 4.7  │
│  │ image │  📍 180 m · 2 min walk  R          │
│  └───────┘  🕘 06:30 - 20:00    3 rev.       │
└──────────────────────────────────────────────┘
```

| Output field | Source | Rule |
|---|---|---|
| Thumbnail | `business_image` (or category artwork) | Lazy-loaded, fixed aspect |
| Name | `business.name` | Escaped |
| Rating | Average of `review` | One decimal; "New" when there are no reviews |
| Distance + walking time | Haversine from the zone anchor | `< 1 km` in metres; ≥ 1 km in km to 1 decimal; walking = metres ÷ 80 |
| Price | `price_level` | R / RR / RRR |
| Today's hours | `business_hours` for the current weekday | "Closed today" on a closed day |
| Open state | Server-calculated `openNow` | "Open now" / "Closed" |
| Result count | API `total` | Always visible |

### 4.7.3.3 Business profile output (`#/business/{id}`)

```
┌──────────────────────────────────────────────┐
│ (←)            [ hero image ]        [OPEN]  │
├──────────────────────────────────────────────┤
│ [STUDY & SOCIAL SPACES]                      │
│ Richfield Campus Library                     │
│ ☆ 4.7 (3)   📍 20 m · 1 min walk   R         │
│                                              │
│ [ Get Directions ]  [ ☎ ]  [ 🌐 ]            │
│                                              │
│ The on-campus library: quiet floors, group   │
│ rooms and reliable Wi-Fi…                    │
├──────────────────────────────────────────────┤
│ Opening hours                  07:30 - 21:00 │
│  Mon 07:30 – 21:00                           │
│  Tue 07:30 – 21:00        ← today emphasised │
│  …  Sun Closed                               │
├──────────────────────────────────────────────┤
│ Amenities                                    │
│  ⚡ Backup Power   🌐 Free Wi-Fi              │
│  ❄ Quiet Atmosphere  🔌 Power Outlets        │
├──────────────────────────────────────────────┤
│ Location   ╭────────────────────────╮        │
│            │   ◎ 1 km ring, pins    │        │
│            ╰────────────────────────╯        │
│ 📍 291 Helen Joseph Street, Pretoria Central │
│ ☎ 012 000 0001   👤 Managed by Lerato M…     │
├──────────────────────────────────────────────┤
│ Reviews                        [ Leave a ★ ] │
│  4.7   ████████████ 5★ 2                     │
│  3 reviews ██████ 4★ 1   ██ 3★ 0 …           │
│  MR Murangi Ruhulani  ★★★★★ 2 days ago ✓     │
│  "Third floor is silent and there are…"      │
└──────────────────────────────────────────────┘
```

| Output block | Content | Notes |
|---|---|---|
| Hero + status | Photo, back button, "Open now"/"Closed" | Status repeats in text |
| Identity | Category badge, name, rating + count, distance, price | Rating shows "New" if unreviewed |
| Actions | Get Directions (external maps), Call (`tel:`), Share | Native behaviour, no dead links |
| Opening hours | Seven rows, today emphasised, closed days labelled | Derived from `business_hours` |
| Amenities | Icon + label chips | From the join table |
| Location | Interactive OpenStreetMap + address, phone, managing owner | Venue pins come from the backend; map tiles need an internet connection |
| Reviews | Average, star distribution bars, each review with initials, stars, relative date, verified badge, comment | One review per user |
| Events | Upcoming events at this venue (when present) | Date, free/paid badge |

### 4.7.3.4 Events output (`#/events`)

```
┌──────────────────────────────────────────────┐
│ Plan your week          [All][This week][…]  │
│ What's on                                    │
├──────────────────────────────────────────────┤
│ [EVENTS & ENTERTAINMENT]            [FREE]   │
│ Friday Night Market                          │
│ Forty food and craft stalls around the…      │
│ 📅 Friday 30 Oct, 17:00                      │
│ 📍 Church Square Night Market                │
│ [ See the venue ]   [ ♡ Save ]               │
└──────────────────────────────────────────────┘
```

| Field | Rule |
|---|---|
| Category badge | From the venue's category |
| Price badge | "Free" (green) when `ticketPrice = 0`, otherwise "R180" (gold) |
| Title / description | Escaped user content |
| Date and time | Long format, locale-friendly |
| Venue | Business name, linking through to the profile |
| Filters | All · This week (7 days) · This month (31 days) · Free entry · Saved |
| Empty state | Nothing scheduled in the selected window, or no events saved on this device |
| Save | Toggles an event in the browser's saved-event list on this device |

### 4.7.3.5 Map output (`#/map`)

* Interactive OpenStreetMap view of the **1 km pilot zone** with a boundary ring and a gold anchor
  dot for the Richfield campus.
* One pin per live listing, positioned from real stored coordinates returned by the backend, clickable through to the profile.
* Legend: *Listing* · *1 km pilot zone* · *26 places*.
* Under the map, the same listings as rows ordered by walking distance ("Closest to the campus").

Because the map is data-driven, it stays correct when listings are added or approved — a listing
outside the zone simply never appears. OpenStreetMap tiles need an internet connection.

### 4.7.3.6 Reports output (`#/reports`)

Five reports, each with a title, a one-line purpose, a generation timestamp, a row count, headline
statistics, a data table with a **TOTAL** row, and **Download CSV** / **Print**.

| # | Report | Question it answers | Columns |
|---|---|---|---|
| 1 | Listings by category | Where is supply concentrated, and how much is stuck in review? | Category · Live · Pending · Rejected · Total · Average rating · Reviews |
| 2 | Verification audit | What is the submission status of every listing, and why? | Business · Category · Owner · Status · Administrator note · Submitted · Distance |
| 3 | Review activity | Is the rating base real and recent? | Business · Category · Reviews · Average · 5 star · 4 star · 3 star · 2 star · 1 star |
| 4 | Owner performance | Which owners deserve promotion or support? | Owner · Email · Listings · Live · Pending · Events · Reviews received · Average rating |
| 5 | Events schedule | What is coming up, and is the calendar alive? | Event · Venue (business) · Room / area · Date and time · Tickets · Category |

**Sample output (report 1, seeded demo data):**

| CATEGORY | LIVE | PENDING | REJECTED | TOTAL | AVG RATING | REVIEWS |
|---|---|---|---|---|---|---|
| Food & Drinks | 6 | 1 | 0 | 7 | 4.4 | 10 |
| Restaurants & Sit-Down Eateries | 5 | 0 | 0 | 5 | 4.5 | 10 |
| Salons & Personal Care | 5 | 1 | 0 | 6 | 4.5 | 9 |
| Study & Social Spaces | 5 | 0 | 1 | 6 | 4.3 | 9 |
| Events & Entertainment | 5 | 0 | 0 | 5 | 4.5 | 7 |
| **TOTAL** | **26** | **2** | **1** | **29** | **4.4** | **45** |

**CSV output** — header row, one row per entity, a TOTAL row, and a dated filename
(`listings-by-category-2026-10-30.csv`):

```csv
Category,Live,Pending,Rejected,Total,Average rating,Reviews
Food & Drinks,6,1,0,7,4.4,10
Restaurants & Sit-Down Eateries,5,0,0,5,4.5,10
...
TOTAL,26,2,1,29,4.4,45
```

### 4.7.3.7 System feedback output

| Output type | Where it appears | Message example |
|---|---|---|
| Success toast | Bottom of the screen, auto-dismiss | "Thanks — your review is live." · "Password updated." |
| Error toast | Same position, red border | "Could not load events" |
| Offline banner | Fixed at the top, pushes content down | "You are offline — showing saved listings. Anything you tap will work once you reconnect." |
| Skeleton loader | In place of the content while loading | Card outlines, no spinner-only screens |
| Empty state | Icon, heading, explanation, action | "Nothing scheduled in this window" |
| Denied state | Full screen | "That area is for administrators" |
| Validation message | Under the form | "The two passwords do not match." |

### 4.7.3.8 Output samples and screenshots

| Output | Dark build (`docs/screenshots-dark/`) | Light build (`screenshots/`) |
|---|---|---|
| Discover / home | `05-discover-guest.png`, `10-discover-signedin.png` | `01-home.png`, `06-mobile-home.png` |
| Search results | `06-explore.png` | `02-explore-filters.png` |
| Filter sheet | `06b-filters.png` | `02-explore-filters.png` |
| Business profile | `07-business.png` | `03-business-profile.png` |
| Review form | `18-review-modal.png` | — |
| Map | `08-map.png` | — |
| Events | `09-events.png` | `04-events.png` |
| Profile / menu | `11-profile-visitor.png`, `12-profile-admin.png` | `06-mobile-home.png`, `07-mobile-profile.png` |
| Owner dashboard + form | `16-owner.png`, `17-owner-form.png` | `08-owner-dashboard.png`, `09-owner-create-listing.png` |
| Admin verification | `13-admin.png` | `10-admin-verification-queue.png` |
| Reports (table + CSV buttons) | `14-reports.png`, `21-desktop-reports.png` | `11-report-listings-by-category.png`, `12-report-review-activity.png` |
| Backup | `15-backup.png` | — |
| Sign in / Sign up / Verify | `02-signin.png`, `03-signup.png`, `04-verify.png` | — |
| Six-tab shell (Ask added) | `assistant/06-discover-with-six-tabs.png` | — |
| Desktop layout | `20-desktop-discover.png`, `21-desktop-reports.png` | — |
| Assistant (Ask) | `assistant/01-assistant-tab.png`, `02-assistant-reply.png`, `03-assistant-cards.png`, `04-assistant-directory.png`, `05-assistant-desktop.png` | — |

### 4.7.3.9 Assistant output (`#/assistant`)

The assistant is the "Chat" slot of the dark-mode reference. It is delivered as a normal screen in
the same shell — same top bar, same 520 px column, same near-black surfaces, same terracotta accent —
rather than as a second application with its own look.

| Element | Design decision | Reused styles |
|---|---|---|
| Screen header | Same greeting block as Discover: small label over a large title, with a green "Online" status badge on the right | `.greet`, `.badge--green` |
| Scope line | One line stating what the guide covers: "1 km around Richfield, 291 Helen Joseph Street — answers from a curated student directory" | `.field__hint` |
| Conversation | Scrollable transcript; assistant replies on the left in a surface-2 bubble, the student's questions on the right in the terracotta bubble; 30 px avatar with the sparkle icon; a timestamp under each reply | `.msg`, `.msg__bubble`, `.msg__avatar` |
| Reply formatting | Paragraphs stay paragraphs, "• " lines become a real bulleted list with the accent marker; text is escaped before it is rendered, so a reply can never inject markup | `.msg__list` |
| Typing state | Three animated dots inside a normal bubble, announced through `aria-live="polite"` while the answer is prepared | `.msg__dots` |
| Composer | Rounded pill with a circular accent send button, directly under the transcript so it never floats over other content | `.composer`, `.input` |
| Suggested places | The reply's places are listed under "Places mentioned" as horizontal cards: thumbnail or category glyph, name, category, address, distance badge, and two actions — *Directions* (walking route from the campus gate) and *Ask about it* | `.place-card`, `.badge--violet`, `.badge--gold` |
| Location feedback | When a visitor returns from an opened Directions link, the assistant asks whether they reached the place and whether its mapped location was accurate; the response is stored locally in that browser | `.msg__feedback` |
| Honesty label | A place whose position has not been confirmed shows a gold **Unverified** badge and no distance; confirmed places show the straight-line metres from campus. The guide never invents a price, an opening time or a distance | `.badge--gold` |
| Directory | "Browse the guide" repeats the Discover chip language and lists the whole curated set, filterable by the nine student categories | `.chips`, `.chip` |
| Failure states | A rate-limited request or a dropped connection shows a toast and an in-transcript explanation; the rest of the app keeps working | `.toast`, `.notice` |

Output rule: the assistant answers only from its curated directory and says so when it does not know
("I don't have confirmed current charges for …"), instead of guessing. Lists are capped at eight
entries so a reply never becomes an unreadable wall of text.

---

### Summary

The interface design follows one consistent model in both builds: a six-item primary menu that
changes with the user's role, full-screen tasks for anything linear (sign in, register, verify),
modal overlays for anything that should not lose context (filters, reviews, confirmations), a single
validation-message channel that surfaces client, API, business-rule and database errors in the same
plain English, and card-based outputs that show the same fields in the same order on every screen.
Every screen is reachable by URL, usable from 360 px to desktop, and has defined loading, empty,
error and permission-denied states.
