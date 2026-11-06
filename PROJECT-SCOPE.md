# CBDVentures — the project in plain English

**CBD Ventures — Pretoria CBD in Your Pocket**
IT Project · Group 5 · Supervisor: Ma'am Ntokozo · Richfield Graduate Institute of Technology

> **Who this document is for:** anyone who needs to understand *what this project is and what it
> does* without reading code — a supervisor, an examiner, a family member, a first-year student, or
> a new team member joining next semester.
>
> You do not need to know what a database, a server or an API is. Every technical word is explained
> the first time it appears, and there is a glossary at the end.
>
> The other README files explain *how to install and run* the software. This one explains *why it
> exists and what it covers*.

---

## 1. The idea in one paragraph

Students arriving in Pretoria Central — many of them new to the city — have no reliable way to find
out what is actually around their campus. Is there a cheap place to eat within walking distance? A
shop that prints assignments? A library with quiet seats? Which of those places are still open, and
which are worth the walk? **CBDVentures is a small phone-sized website that answers those questions
for one square kilometre around the Richfield campus.** It shows local businesses, lets students
review them, lets business owners manage their own listing, and gives the administrator a way to
check that listings are real before anyone relies on them.

---

## 2. The problem we set out to solve

Picture a first-year student on their first week at Richfield. They have R50, an assignment due
tomorrow, and no idea where anything is. Today they would probably:

* walk around hoping to spot something,
* ask in a WhatsApp group and wait for an answer,
* or open a general map app that shows every shop in the city but tells them nothing about
  *student* prices, printing, plug points or quiet study space.

General map apps are built for drivers and tourists. They are not built for a student with R50 and
an hour between lectures.

**The gap:** there is no single, trustworthy, hyper-local view of the few streets that matter to
Richfield students.

**Our answer:** shrink the map. Instead of covering a whole city, CBDVentures covers **1 km around
the campus gate** and does three things with that small area:

1. **Lists what is there** — food, groceries, health, printing, study space, banking, grooming,
   transport and entertainment.
2. **Keeps it honest** — every listing is checked by an administrator before it is published, and
   student-submitted places are clearly labelled as unconfirmed.
3. **Makes it usable on a phone, on a student budget, on a slow connection** — one hand, quickly,
   with the essentials still visible when the signal drops.

---

## 3. Who it is for

| Who | What they want | What CBDVentures gives them |
|---|---|---|
| **Students and local residents** ("visitors") | To find something nearby, fast, and know it is real | Browse and search by category, see distance in metres and walking minutes, read reviews, leave a review |
| **Business owners** | Their own shop to appear correctly, without paying an agency | A sign-in, a dashboard, and forms to create, edit and delete their own listing and publish events |
| **The platform administrator** | Nothing fake or broken on the platform | A verification queue to approve or reject new listings, five reports with CSV downloads, and a full backup of the system |
| **The project team** | Proof that it works | 86 automated checks that run in well under a minute, plus a written record of every requirement |

A person can also browse the whole site **without an account** — signing in is only needed to review,
to manage a listing, or to administer the platform. That was a deliberate choice: the fastest way to
lose a student user is to demand a password before they have seen anything useful.

---

## 4. What it does, in ordinary language

### If you are a student

* Open the site and land on **Discover**: a greeting, a search box, category chips, and a rail of
  featured places near campus.
* Ask for something specific in **Explore** — "kota", "printing", "quiet" — and narrow it down by
  category, price, distance and rating.
* Open a business to see photos, what it sells, its hours for today, its amenities (card payments,
  Wi-Fi, plug points, wheelchair access), a map pin, and directions that open in your phone's maps
  app.
* Read other students' reviews. Leave one of your own — **one per person**, editable once a day, so
  the ratings cannot be gamed by a burst of fake reviews. You cannot review your own shop.
* See what is on this week in **Events**.
* Use the **Ask** tab — a student guide you can type questions into: *"I'm hungry, where can I
  eat?"*, *"somewhere to study"*, *"what can I buy with R50?"*. It answers from a curated list of 19
  nearby places, tells you how far each one is, and **says so when it does not know** a price or an
  opening time rather than inventing one.
* If your signal drops, the listings you already opened stay readable, and a banner tells you that
  you are offline.

### If you own a business

* Sign in and open your dashboard. You see **only your own listings**.
* Add a listing: name, category, address, price level, opening hours for each day, amenities,
  description. The address must fall inside the pilot area — if it does not, the form tells you how
  far outside you are instead of just refusing.
* A new listing arrives as **Pending review**. It is not visible to the public until the
  administrator approves it. You can see its status at any time.
* Add events to your listing (date, time, free or paid).

### If you are the administrator

* A **verification queue** of submissions, each with the owner's details, the address and the
  measured distance from campus. Approve or reject, with a note.
* **Five reports**, each with headline numbers and a **CSV download** you can open in Excel:
  listings by category, verification audit, review activity, owner performance, event schedule.
* **Backup and restore**: download every table as one file; restore it again in one step. This is the
  "what if the laptop dies the night before the demo" insurance policy.

---

## 5. What is inside (the six tabs, plus the owner and admin areas)

The public part of the app is a bottom tab bar, the way a phone app works:

| Tab | What it is for |
|---|---|
| **Discover** | The home screen: greeting, search, categories, featured places, what is popular, what is on soon |
| **Explore** | Search and filter the full list, with a filter sheet you can open over the page |
| **Map** | One square kilometre around campus with every live listing pinned |
| **Ask** | The student assistant — a chat with suggested places you can act on |
| **Events** | What is happening this week, this month, or for free |
| **Profile** | Your account, and (for owners and administrators) the doors to their tools |

Behind **Profile** sit four more areas that ordinary visitors never see: the **owner dashboard**, the
**verification queue**, **reports**, and **backup**. Access is checked twice — the menu hides what you
may not use, and the server refuses it even if you type the address by hand.

---

## 6. Scope: what is in, what is out, and why

Being clear about the edges of a project is as important as the features. This is the honest list.

### Delivered (in scope)

* A working web application that runs with **one command** and needs nothing else installed for a
  demo (its own small database is built in).
* Discovery and search inside a **1 km radius** of the Richfield Pretoria campus.
* Business profiles with hours, amenities, photos, map pin and directions.
* Reviews, one per person, editable once per 24 hours, with owners blocked from reviewing themselves.
* An owner portal for creating, editing and deleting **own** listings, plus events.
* Administrator verification of new listings, five reports with CSV export, and backup/restore.
* Personalised suggestions based on the preferences a signed-in student saves.
* **Email verification at sign-up** (a six-digit code), so a fake address cannot create an account.
* Three user roles (student, business owner, administrator) enforced on the server.
* Two visual designs of the same application: a **light portal** layout and a **dark phone app**
  layout, so the interface could be judged against the supplied reference screens.
* The **Ask** assistant, built from the team's separate assistant project and folded into the app.
* A MySQL database design for production use, with 12 tables and rules enforced inside the database
  itself, not only in the application.
* 86 automated tests, and written documentation (interface design, requirements traceability,
  database design, backup procedure, panel preparation).

### Deliberately not delivered (out of scope, with reasons)

| Not built | Why not |
|---|---|
| **Payments** (the "Pay" tab on the reference screens) | There is no payment model anywhere in our requirements or database. A button that pretends to take money would be worse than no button. |
| **A native Android or iOS app** | The requirement was a mobile-friendly experience. A well-built web app opens on any phone with no app store, no install and no update cycle — which also made it markable on the spot. |
| **Real-time chat between students** | Moderating live messages is a project on its own. The Ask tab answers questions about places instead. |
| **Live business-directory feeds from map providers** | Venue records stay curated in the CBDVentures database. The map screen uses OpenStreetMap tiles behind those stored records and needs an internet connection to show its map background. |
| **The full 5 km extended area in the interface** | The database already classifies listings as inside 1 km or the wider 5 km zone, and a single settings flag switches the boundary. The interface ships with 1 km because that is the stated pilot area; turning it on is a configuration change, not a rebuild. |
| **A public launch** | This is a pilot for one campus. Going live needs real businesses to sign up and someone to run the verification queue — an operational job, not a coding one. |

### Sensible next steps after hand-over

1. Ask the **real** Richfield businesses to claim their listings, so the data is owned by them.
2. Switch the pilot radius to 5 km and measure whether the listings still feel local and useful.
3. Replace the demo email delivery with a real mail service (the code already exists; only the
   sending channel is simulated).
4. Add photo uploads from owners, so listings age better than typed descriptions.
5. Measure: how many students use it in a week, and how many walk to a place they found here.

---

## 7. How it is built, in plain language

Think of a restaurant:

* **The dining room** is the part you see and touch — the screens, buttons and colours. This is the
  *front end* (HTML, CSS and JavaScript).
* **The kitchen** is where the work is done: checking your password, calculating distance, deciding
  what a search should return. This is the *back end* (Java, using a framework called Spring Boot).
* **The stockroom** is where everything is kept between visits: the listings, reviews, accounts and
  events. This is the *database* (H2 while demonstrating, MySQL for a real installation).

The three talk to each other over a **REST API** — a fixed set of requests, like a waiter taking a
written order to the kitchen and bringing back a plate. Every request has a predictable answer, in a
predictable format, which is why a screen can be rebuilt without touching the rules behind it.

Some choices worth knowing about, in plain terms:

* **One command to run.** The dining room and the kitchen sit in the same building, so starting the
  application starts everything on a single address (`http://localhost:8080`).
* **Nothing to install on a phone.** No app store, no downloads: it is a website that behaves like an
  app, including a shortcut on the home screen.
* **Two designs, one engine.** The light and dark versions share the same brain. Only the "dining
  room" was redecorated, which is why the test results for both are near-identical.
* **The assistant is not a paid AI service.** Our Ask tab is a rule-based guide reading a curated
  list of places. It costs nothing per question, works with no internet connection, and cannot
  invent a shop that does not exist. When a language-model service was optional in the original
  assistant project, we left it out on purpose: a project that breaks when an API key expires is not
  a reliable demo.

---

## 8. How it protects people

| Concern | What we did |
|---|---|
| **Passwords** | Stored scrambled (BCrypt hashing), never as text. Nobody — including the administrator — can read a student's password. |
| **Fake accounts** | A six-digit code must be entered before a new account works. The code expires in 10 minutes, five wrong guesses cancel it, and requesting a new one too quickly is blocked. |
| **Fake or wrong listings** | Nothing is published until an administrator approves it, and student-submitted places are labelled as unconfirmed. |
| **Rigged ratings** | One review per person per business, editable once a day, and owners cannot review their own shop. |
| **Reaching things you should not** | Three roles, checked on the server for every request — hiding a menu item is only a convenience, never the protection. |
| **Personal information (POPIA)** | We collect a name and an email address, nothing more. No ID numbers, no addresses, no payment details. |
| **Distances that are guesses** | Distances are calculated from real map coordinates to the campus gate. When a place's position has not been confirmed, the app shows no number at all rather than a made-up one. |

---

## 9. The project at a glance

| | |
|---|---|
| Pilot area | 1 km around Richfield Pretoria Campus, 291 Helen Joseph Street |
| Categories | 5 for listings, 9 in the student assistant's directory |
| Seeded demo data | 29 listings (26 live, 2 awaiting review, 1 rejected), 45 reviews, 12 events, 14 amenities, 17 accounts |
| Assistant directory | 19 curated student places, 7 with photographs, 6 with confirmed positions |
| User roles | Student (visitor), business owner, administrator |
| Database (production) | 12 tables, 15 relationships, 21 indexes, 11 built-in value checks, 8 triggers |
| Automated tests | 86, all passing (well under a minute) |
| Documentation | Interface design (Section 4.7), database design, backup procedure, requirement traceability, panel preparation |
| Runs on | Any computer with Java 25 — no installation wizard, no app store |

---

## 10. Seeing it in two minutes

If someone hands you the project and asks "show me", this is the shortest path:

1. **Start it.** One command; open the address it prints. You land on the welcome screen.
2. **Browse as a guest.** Discover → tap a place → hours, amenities, map, reviews. No account needed.
3. **Sign in as a student** (`keziah@student.richfield.ac.za` / `Password123!`) and leave a review.
4. **Sign in as an owner** (`thabo@kotakingdom.co.za`) and add a listing. It appears as *Pending*.
5. **Sign in as the administrator** (`admin@cbdventures.co.za` / `Admin123!`) and approve it, then
   open Reports and download a CSV.
6. **Ask tab:** type *"I'm hungry, where can I eat?"*, then *"what can I buy with R50?"*.

The full demo script, including the exact click path and the bugs we found and fixed along the way,
is in `app/dark-mode/DARK-MODE.md`.

---

## 11. Words you will meet, explained

| Word | What it actually means here |
|---|---|
| **Repository** | The folder holding every file of the project, with its history. |
| **Server** | The program that runs on a computer and answers requests from phones and browsers. |
| **Database** | Where the app keeps its information — the stockroom. |
| **Table** | One kind of thing stored in the database: one table of businesses, one of reviews, one of accounts. |
| **API** | The agreed set of requests the screens are allowed to make ("give me the listings in this category"). |
| **Front end / back end** | What you see, versus what works behind it. |
| **JWT / token** | A short-lived pass the app gives you after sign-in, so you do not re-type your password on every tap. |
| **Hashing** | Scrambling a password in a way that cannot be reversed. |
| **Seed data** | Believable example information loaded on first start, so the app is never empty during a demo. |
| **Test** | A small automated check that proves a rule still works after a change — ours cover distance maths, opening hours, search, security, reports and the assistant. |
| **Offline cache** | A copy of what you already looked at, kept on your phone so it still shows when the signal drops. |
| **POPIA** | South Africa's data-protection law; here it shapes how little personal information we ask for. |
| **Pilot** | A deliberately small first release — one campus, one square kilometre — meant to prove the idea before growing it. |

---

## 12. Frequently asked questions

**Is this a real business?**
No. It is an academic project: a working pilot built to be demonstrated and marked. Every business in
the demo data is used as an example of what the platform would carry.

**Could it be used for real?**
Yes, with three practical additions: real business owners claiming their listings, a real email
service for the verification codes, and someone to run the approval queue. The application, the
database design and the security rules are already in place.

**Why only one square kilometre?**
Because "everything in Pretoria" is useless to a student with an hour between lectures. A small area
can be complete, checked and trustworthy — the campus library, a kota shop and a printing place all
within a ten-minute walk. The database already supports a 5 km extended area behind a single setting.

**Why are there two versions of the screens?**
The brief supplied reference screens for a dark, phone-shaped design while the earlier prototype was a
light desktop portal. Rather than throw one away, both were built on the same engine, so the team can
show the design it was asked to match and still demonstrate the original layout.

**What was the hardest part?**
Keeping the data honest: distances that are measured rather than guessed, listings that are approved
rather than assumed, prices the assistant refuses to invent, and reviews that cannot be stacked. Most
of the visible polish exists to support that one idea.

---

*Questions about a specific part of the system should go to the team member who owns that module —
the per-developer panel questions document (`docs/panel-questions-by-developer.md`) names each module
and what to prepare for it.*
