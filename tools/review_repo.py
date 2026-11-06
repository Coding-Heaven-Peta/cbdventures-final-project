#!/usr/bin/env python3
"""
Review a CBDVentures codebase and produce a prioritised fix list mapped to the
Richfield group-project evaluation sheet (80 group marks).

Usage:
    python3 tools/review_repo.py --path /path/to/their/repo --out review.md
    python3 tools/review_repo.py --path . --json review.json

Each of the nine standards is scored from evidence found on disk, and every
missing check becomes an ordered action with: what to add, where to add it,
how many marks are at stake, and how much work it is.
"""

from __future__ import annotations

import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from compare_projects import STANDARDS, profile  # noqa: E402  (shared signal engine)

# --------------------------------------------------------------------------
# Remediation guidance: signal key -> (action, where, effort S/M/L)
# --------------------------------------------------------------------------

FIXES: dict[str, tuple[str, str, str]] = {
    # 1 Running System
    "build_file": ("Commit the build file so the project can be compiled by the marker",
                   "repository root (pom.xml / build.gradle / package.json)", "S"),
    "entrypoint": ("Add a single entry point that starts the whole application",
                   "src/main/java/... (a class with @SpringBootApplication)", "M"),
    "run_docs": ("Write a 'How to run' section: exact command, port, and demo logins",
                 "README.md", "S"),
    "frontend_present": ("Serve the HTML interface from the application itself",
                         "src/main/resources/static/index.html", "M"),

    # 2 Validation
    "bean_validation": ("Annotate every request DTO (@NotBlank, @Email, @Size, @Min/@Max) and validate the body (@Valid)",
                        "web/dto/*.java and the controller methods", "M"),
    "client_validation": ("Add required/pattern/type attributes on inputs and check them in JS before submitting",
                          "the HTML forms and their submit handlers", "S"),
    "error_handler": ("Catch exceptions centrally and return a readable JSON message instead of a stack trace",
                      "error/GlobalExceptionHandler.java (@RestControllerAdvice)", "S"),
    "business_rules": ("Move business rules from the UI into the service layer and throw typed exceptions "
                       "(e.g. one review per user, owner cannot review own business, rating 1-5)",
                       "service/*.java", "M"),

    # 3 Database
    "schema_file": ("Commit an executable SQL schema (CREATE TABLE ... with comments) for the marker to run",
                    "db/schema.sql", "M"),
    "orm_entities": ("Model each table as an entity class instead of raw SQL strings",
                     "domain/*.java (@Entity)", "L"),
    "foreign_keys": ("Define real relationships (FK columns + @ManyToOne/@OneToMany) instead of storing bare id numbers",
                     "domain/*.java and the SQL schema", "M"),
    "constraints": ("Add database-level constraints: NOT NULL, UNIQUE, CHECK (rating BETWEEN 1 AND 5), ENUM, "
                    "ON DELETE CASCADE/RESTRICT",
                    "db/schema.sql", "M"),
    "indexes": ("Index the columns you filter and sort on (status, category_id, event date)",
                "db/schema.sql (CREATE INDEX)", "S"),

    # 4 Form links
    "html_forms": ("Create the data-capture screens (register, login, listing, review)",
                   "the front-end pages", "M"),
    "form_to_api": ("Submit forms with fetch() to the REST API and handle success/failure in the UI",
                    "js/api.js and each view module", "M"),
    "crud_endpoints": ("Expose create/update/delete endpoints for the entities users manage",
                       "web/*Controller.java", "M"),

    # 5 Backup
    "backup": ("Add a backup facility: export every table to one file (JSON or SQL) from an admin screen",
               "service/BackupService.java + admin view", "M"),
    "restore": ("Add the matching restore: upload the backup file and reload it inside one transaction",
                "service/BackupService.java (restore) + admin view", "M"),

    # 6 Reports
    "reports": ("Build 3-5 management reports (listings per category, verification audit, review activity, "
                "owner performance, events schedule)",
                "service/ReportService.java + a reports screen", "L"),
    "csv_export": ("Let each report download as CSV (text/csv + Content-Disposition) so it can go in the project file",
                   "the report endpoint", "S"),

    # 7 Login and security
    "login": ("Implement sign-in that issues a token or session for the signed-in user",
              "service/AuthService.java + web/AuthController.java", "L"),
    "hashing": ("Hash passwords with BCrypt (never store or log plain text)",
                "the registration and login path", "S"),
    "authorisation": ("Add roles (VISITOR / OWNER / ADMIN) and enforce them on routes and on each record "
                      "(an owner may only edit their own listing)",
                      "config/SecurityConfig.java + service checks", "M"),
    "route_guard": ("Return 401 for 'not signed in' and 403 for 'not allowed', and have the UI react to each",
                    "security configuration and the API client", "S"),

    # 8 User friendliness
    "responsive": ("Add media queries so the layout works from 360 px phones to desktop",
                   "css/app.css", "M"),
    "accessibility": ("Add labels, aria attributes, alt text, focus styles and a skip link",
                      "the HTML templates", "S"),
    "offline": ("Add a manifest + service worker so the shell and saved listings work offline",
                "static/manifest.webmanifest and static/sw.js", "M"),
    "ux_feedback": ("Show toasts, skeletons, empty states and inline errors so nothing fails silently",
                    "ui.js + each view", "S"),

    # 9 Modules
    "tests": ("Add automated tests for the core rules (distance, filters, review rules, auth)",
              "src/test/java", "L"),
    "docs": ("Write a README plus API reference and a requirements traceability table",
             "README.md and docs/", "M"),
    "screenshots": ("Capture screenshots of every screen for the project file",
                    "screenshots/", "S"),
}


def review(project: dict) -> dict:
    """Turn a profile into a prioritised remediation plan."""
    gaps = []
    for standard, marks in STANDARDS:
        checks = [c for c in project["signals"] if c["standard"] == standard]
        missing = [c for c in checks if not c["present"]]
        if not missing:
            continue
        # marks at stake: the share of this standard that is unsupported
        at_stake = round(marks * len(missing) / len(checks), 1)
        for check in missing:
            action, where, effort = FIXES.get(
                check["key"], ("Add evidence for this check", "project", "M"))
            gaps.append({
                "standard": standard,
                "standard_marks": marks,
                "marks_at_stake": round(at_stake / len(missing), 1),
                "check": check["label"],
                "key": check["key"],
                "action": action,
                "where": where,
                "effort": effort,
            })

    effort_rank = {"S": 0, "M": 1, "L": 2}
    gaps.sort(key=lambda g: (-g["marks_at_stake"], effort_rank[g["effort"]], g["standard"]))

    quick_wins = [g for g in gaps if g["effort"] == "S"]
    return {"project": project, "gaps": gaps, "quick_wins": quick_wins}


def render(result: dict) -> str:
    project = result["project"]
    gaps = result["gaps"]
    lines = []
    add = lines.append

    add(f"# Code review — `{project['name']}`\n")
    add(f"{project['file_count']} files · indicative score "
        f"**{project['indicative_total']}/{project['marks_total']}**\n")
    add("Language mix: " + ", ".join(f"{k} {v}" for k, v in project["languages"].items()) + "\n")

    add("## Scorecard\n")
    add("| Standard | Marks | Supported | Indicative | Verdict |")
    add("|---|---|---|---|---|")
    for standard, marks in STANDARDS:
        row = project["standards"][standard]
        pct = int(row["strength"] * 100)
        verdict = "strong" if pct >= 75 else "partial" if pct >= 40 else "**at risk**"
        add(f"| {standard} | {marks} | {row['checks_passed']}/{row['checks_total']} | "
            f"{row['indicative_score']}/{marks} | {verdict} |")
    add("")

    if not gaps:
        add("No gaps detected — every check has evidence on disk.\n")
        return "\n".join(lines)

    add(f"## Prioritised fix list ({len(gaps)} items)\n")
    add("Ordered by marks at stake, then by effort. `S` ≈ under an hour, `M` ≈ half a day, `L` ≈ a day or more.\n")
    add("| # | Marks | Effort | Fix | Where |")
    add("|---|---|---|---|---|")
    for index, gap in enumerate(gaps, 1):
        add(f"| {index} | +{gap['marks_at_stake']} | {gap['effort']} | {gap['action']} | `{gap['where']}` |")
    add("")

    if result["quick_wins"]:
        add("## Quick wins first\n")
        add("These cost the least effort for the most marks:\n")
        for gap in result["quick_wins"]:
            add(f"- **{gap['check']}** (+{gap['marks_at_stake']}) — {gap['action']} *({gap['where']})*")
        add("")

    add("## Notes\n")
    add("- A `no` means **no evidence found on disk**, not a confirmed zero. Check by hand before rewriting anything.")
    add("- This models the sheet's group section (80 marks); the individual 20 marks are presentation marks.")
    add("- Detected files include documentation, so a feature described only in `docs/` still counts as "
        "`yes` — make sure the code actually backs it up in the demo.")
    add("")
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser(description="Review a codebase against the evaluation sheet.")
    parser.add_argument("--path", default=".", help="project to review (default: .)")
    parser.add_argument("--out", help="write the markdown review here")
    parser.add_argument("--json", dest="json_out", help="write the raw review as JSON here")
    args = parser.parse_args()

    result = review(profile(args.path))
    markdown = render(result)

    if args.out:
        with open(args.out, "w", encoding="utf-8") as handle:
            handle.write(markdown)
        print(f"Review written to {args.out}")
    else:
        print(markdown)

    if args.json_out:
        with open(args.json_out, "w", encoding="utf-8") as handle:
            json.dump(result, handle, indent=2, default=str)
        print(f"JSON written to {args.json_out}")


if __name__ == "__main__":
    main()
