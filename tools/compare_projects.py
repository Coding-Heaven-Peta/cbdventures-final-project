#!/usr/bin/env python3
"""
Compare two CBDVentures codebases against the Richfield group-project evaluation sheet.

Usage:
    python3 tools/compare_projects.py --theirs /path/to/their/repo
    python3 tools/compare_projects.py --theirs ../CBDVentures-theirs --mine . --out report.md
    python3 tools/compare_projects.py --theirs /path/to/repo --json profile.json

It profiles each project by *detecting evidence on disk* (no assumptions), scores
all nine evaluation standards, and prints what one project has that the other
does not. Nothing is modified.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from collections import Counter
from dataclasses import dataclass, field, asdict

SKIP_DIRS = {
    ".git", "target", "build", "node_modules", "dist", "out", "bin", "obj",
    ".idea", ".vscode", "__pycache__", ".gradle", "venv", ".venv", "coverage",
    ".next", ".nuxt", "data", "vendor",
}
TEXT_EXT = {
    ".java", ".kt", ".js", ".mjs", ".jsx", ".ts", ".tsx", ".html", ".htm",
    ".css", ".scss", ".sql", ".xml", ".yml", ".yaml", ".json", ".md", ".txt",
    ".properties", ".cs", ".php", ".py", ".env", ".sh", ".gradle",
}
MAX_BYTES = 400_000


# --------------------------------------------------------------------------
# Signal definitions: (key, label, evaluation standard, [regex patterns])
# --------------------------------------------------------------------------

@dataclass
class Signal:
    key: str
    label: str
    standard: str
    patterns: list[str]
    hits: list[str] = field(default_factory=list)

    @property
    def present(self) -> bool:
        return bool(self.hits)


SIGNALS = [
    # 1. Running system ---------------------------------------------------
    Signal("build_file", "Build file (pom/gradle/package/requirements)", "Running System",
           [r"(?i)(spring-boot-starter-parent|<artifactId>spring-boot|build\.gradle|"
            r"\"dependencies\"\s*:|flask|django|express)"]),
    Signal("entrypoint", "Application entry point / server bootstrap", "Running System",
           [r"(?i)(@SpringBootApplication|public\s+static\s+void\s+main|"
            r"app\.listen\(|if\s+__name__\s*==\s*.__main__.|CreateBuilder\()"]),
    Signal("run_docs", "Run instructions documented", "Running System",
           [r"(?i)(mvnw|mvn\s+spring-boot:run|npm\s+(run\s+)?(dev|start)|"
            r"localhost:8080|localhost:3000|how to run|getting started)"]),
    Signal("frontend_present", "HTML/CSS/JS front end present", "Running System",
           [r"(?i)!doctype html"]),

    # 2. Validation -------------------------------------------------------
    Signal("bean_validation", "Server-side validation annotations", "Validation",
           [r"(?i)(jakarta\.validation|javax\.validation|@NotNull|@NotBlank|@Size\(|@Email|@Min\(|@Max\(|@Valid)"]),
    Signal("client_validation", "Client-side validation (required/pattern/JS checks)", "Validation",
           [r"(?i)(required\b|pattern=|type=\"email\"|<input[^>]+min=)" ]),
    Signal("error_handler", "Central error handling with readable messages", "Validation",
           [r"(?i)(@ControllerAdvice|@RestControllerAdvice|@ExceptionHandler|"
            r"errorhandler|try\s*\{[^}]*catch)"]),
    Signal("business_rules", "Business rules enforced in code (not just UI)", "Validation",
           [r"(?i)(throw new \w*Exception|IllegalArgumentException|BadRequest|forbidden|"
            r"cooldown|one review per|already reviewed|outside the .* zone)"]),

    # 3. Database ---------------------------------------------------------
    Signal("schema_file", "SQL schema / migration file", "Database",
           [r"(?i)(CREATE TABLE|CREATE DATABASE|ALTER TABLE)"]),
    Signal("orm_entities", "Entity/model classes", "Database",
           [r"(?i)(@Entity|@Table\(|modelBuilder\.Entity|db\.Model|class\s+\w+\s*\(\s*models\.Model)"]),
    Signal("foreign_keys", "Foreign keys / relationship mapping", "Database",
           [r"(?i)(@ManyToOne|@OneToMany|@ManyToMany|@JoinColumn|FOREIGN KEY|"
            r"hasMany|belongsTo|REFERENCES\s+\w+)"]),
    Signal("constraints", "Integrity constraints (UNIQUE/CHECK/ENUM/NOT NULL)", "Database",
           [r"(?i)(UNIQUE\s*\(|CHECK\s*\(|ENUM\(|NOT NULL|@Column\([^)]*nullable\s*=\s*false|"
            r"@UniqueConstraint)"]),
    Signal("indexes", "Indexes defined", "Database", [r"(?i)(CREATE INDEX|@Index\()"]),

    # 4. Form links -------------------------------------------------------
    Signal("html_forms", "HTML forms", "Form Links", [r"(?i)<form\b"]),
    Signal("form_to_api", "Forms wired to backend calls", "Form Links",
           [r"(?i)(fetch\(|axios\.|XMLHttpRequest|@PostMapping|@PutMapping|@DeleteMapping|"
            r"\.submit\(|form\.serialize)"]),
    Signal("crud_endpoints", "CRUD endpoints (create/update/delete)", "Form Links",
           [r"(?i)(@PostMapping|@PutMapping|@DeleteMapping|router\.(post|put|delete)|"
            r"HttpPost|HttpPut|HttpDelete)"]),

    # 5. System backup ----------------------------------------------------
    Signal("backup", "Backup feature", "System Backup",
           [r"(?i)(backup|mysqldump|\.mv\.db|restore|export.*(json|sql)|dump)"]),
    Signal("restore", "Restore / recovery path", "System Backup",
           [r"(?i)(restore|import.*(json|sql)|rollback)"]),

    # 6. Reports ----------------------------------------------------------
    Signal("reports", "Report generation", "Reports (3-5)",
           [r"(?i)(report|summar(y|ise)|statistics|analytics|dashboard.*(chart|total))"]),
    Signal("csv_export", "CSV / printable export", "Reports (3-5)",
           [r"(?i)(text/csv|\.csv|Content-Disposition|ExportToCsv|window\.print)"]),

    # 7. Login and security ----------------------------------------------
    Signal("login", "Login / authentication", "Login and Security",
           [r"(?i)(login|signin|sign-in|authenticate|AuthenticationManager|jwt|session)"]),
    Signal("hashing", "Password hashing (not plain text)", "Login and Security",
           [r"(?i)(BCrypt|PasswordEncoder|password_hash|Argon2|PBKDF2|SHA-?256\s*\(.*password|messageDigest)"]),
    Signal("authorisation", "Role-based authorisation", "Login and Security",
           [r"(?i)(hasRole|hasAuthority|@PreAuthorize|ROLE_|isAdmin|requireRole|authorize\()"]),
    Signal("route_guard", "Guarded routes / 401-403 handling", "Login and Security",
           [r"(?i)(AuthenticationEntryPoint|AccessDeniedHandler|403|401|Unauthorized)"]),

    # 8. User friendliness ------------------------------------------------
    Signal("responsive", "Responsive layout (media queries)", "User Friendliness",
           [r"(?i)(@media\s*\(|viewport|max-width:\s*\d+px)"]),
    Signal("accessibility", "Accessibility affordances", "User Friendliness",
           [r"(?i)(aria-|role=\"|sr-only|skip link|alt=\")"]),
    Signal("offline", "Offline / PWA support", "User Friendliness",
           [r"(?i)(serviceWorker|service-worker|manifest\.(webmanifest|json)|caches\.|offline)"]),
    Signal("ux_feedback", "Toasts / loaders / empty states", "User Friendliness",
           [r"(?i)(toast|skeleton|loading|spinner|empty state|no results)"]),

    # 9. Modules ----------------------------------------------------------
    Signal("tests", "Automated tests", "Modules (at least 2)",
           [r"(?i)(@Test|junit|assertThat|expect\(|describe\(|unittest|pytest)"]),
    Signal("docs", "Written documentation", "Modules (at least 2)",
           [r"(?i)(# Project|## Installation|API reference|requirements traceability)"]),
    Signal("screenshots", "Screenshots / evidence pack", "Modules (at least 2)",
           [r"\.(png|jpg|jpeg|webp)$"]),
]

STANDARDS = [
    ("Running System", 10),
    ("Validation", 10),
    ("Database", 10),
    ("Form Links", 10),
    ("System Backup", 10),
    ("Reports (3-5)", 10),
    ("Login and Security", 10),
    ("User Friendliness", 5),
    ("Modules (at least 2)", 5),
]


# --------------------------------------------------------------------------
# Profiling
# --------------------------------------------------------------------------

SELF = os.path.abspath(__file__)


def walk(root: str):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS and not d.startswith(".git")]
        for name in filenames:
            path = os.path.join(dirpath, name)
            if os.path.abspath(path) == SELF:
                continue          # the comparator is not project evidence
            yield path


def profile(root: str) -> dict:
    root = os.path.abspath(root)
    if not os.path.isdir(root):
        sys.exit(f"Not a directory: {root}")

    files, by_ext = [], Counter()
    for path in walk(root):
        files.append(path)
        by_ext[os.path.splitext(path)[1].lower()] += 1

    # Read text files once, keep a lowercase corpus plus a path list per signal.
    corpus_parts, texts = [], {}
    for path in files:
        ext = os.path.splitext(path)[1].lower()
        if ext not in TEXT_EXT:
            continue
        try:
            if os.path.getsize(path) > MAX_BYTES:
                continue
            with open(path, "r", encoding="utf-8", errors="ignore") as handle:
                text = handle.read()
        except OSError:
            continue
        texts[path] = text
        corpus_parts.append(text)
    corpus = "\n".join(corpus_parts)

    results = []
    for signal in SIGNALS:
        hits = []
        for path, text in texts.items():
            for pattern in signal.patterns:
                if re.search(pattern, text):
                    hits.append(os.path.relpath(path, root))
                    break
        if signal.key == "screenshots":
            hits = [os.path.relpath(p, root) for p in files
                    if os.path.splitext(p)[1].lower() in {".png", ".jpg", ".jpeg", ".webp"}]
        results.append({
            "key": signal.key,
            "label": signal.label,
            "standard": signal.standard,
            "present": bool(hits),
            "evidence": sorted(
                set(hits),
                key=lambda h: (h.startswith(("docs/", "tools/")), len(h))
            )[:6],
            "evidence_count": len(set(hits)),
        })

    per_standard = {}
    for name, marks in STANDARDS:
        checks = [r for r in results if r["standard"] == name]
        passed = sum(1 for c in checks if c["present"])
        strength = passed / len(checks) if checks else 0
        per_standard[name] = {
            "marks_available": marks,
            "checks_total": len(checks),
            "checks_passed": passed,
            "strength": round(strength, 2),
            "indicative_score": round(marks * strength, 1),
        }

    # Language mix by file count (useful when the two projects differ in stack).
    languages = {
        "java": by_ext.get(".java", 0),
        "js_ts": by_ext.get(".js", 0) + by_ext.get(".mjs", 0) + by_ext.get(".ts", 0) + by_ext.get(".tsx", 0),
        "html": by_ext.get(".html", 0) + by_ext.get(".htm", 0),
        "css": by_ext.get(".css", 0) + by_ext.get(".scss", 0),
        "sql": by_ext.get(".sql", 0),
        "csharp": by_ext.get(".cs", 0),
        "python": by_ext.get(".py", 0),
        "php": by_ext.get(".php", 0),
        "kotlin": by_ext.get(".kt", 0),
    }

    return {
        "root": root,
        "name": os.path.basename(root.rstrip(os.sep)),
        "file_count": len(files),
        "by_extension": dict(by_ext.most_common(14)),
        "languages": {k: v for k, v in languages.items() if v},
        "signals": results,
        "standards": per_standard,
        "indicative_total": round(sum(s["indicative_score"] for s in per_standard.values()), 1),
        "marks_total": sum(m for _, m in STANDARDS),
        "top_level": sorted({
            os.path.relpath(p, root).split(os.sep)[0] for p in files
        })[:24],
    }


# --------------------------------------------------------------------------
# Reporting
# --------------------------------------------------------------------------

MARK = {True: "yes", False: "no"}


def render(mine: dict, theirs: dict | None) -> str:
    lines = []
    add = lines.append

    add("# CBDVentures — project comparison against the evaluation sheet\n")
    if theirs:
        add(f"- **Mine:** `{mine['name']}` ({mine['file_count']} files)")
        add(f"- **Theirs:** `{theirs['name']}` ({theirs['file_count']} files)\n")
    else:
        add(f"- **Profile:** `{mine['name']}` ({mine['file_count']} files)\n")

    add("## Stack detected\n")
    add("| Language | Mine | Theirs |")
    add("|---|---|---|")
    keys = sorted(set(mine["languages"]) | set(theirs["languages"] if theirs else set()))
    for key in keys:
        a = mine["languages"].get(key, 0)
        b = theirs["languages"].get(key, 0) if theirs else "—"
        add(f"| {key} | {a} | {b} |")
    add("")

    add("## Evaluation standards\n")
    if theirs:
        add("| Standard | Marks | Mine | Theirs | Gap |")
        add("|---|---|---|---|---|")
    else:
        add("| Standard | Marks | Evidence found | Checks passed | Indicative score |")
        add("|---|---|---|---|---|")
    for name, marks in STANDARDS:
        a = mine["standards"][name]
        if theirs:
            b = theirs["standards"][name]
            gap = round(a["indicative_score"] - b["indicative_score"], 1)
            flag = "" if gap == 0 else ("🔵 mine ahead" if gap > 0 else "🔴 theirs ahead")
            add(f"| {name} | {marks} | {a['indicative_score']}/{marks} | "
                f"{b['indicative_score']}/{marks} | {gap:+.1f} {flag} |")
        else:
            add(f"| {name} | {marks} | {a['checks_passed']}/{a['checks_total']} | "
                f"{int(a['strength'] * 100)}% | {a['indicative_score']}/{marks} |")
    add(f"\n**Indicative total: {mine['indicative_total']}/{mine['marks_total']}**"
        + (f" (mine) vs **{theirs['indicative_total']}/{theirs['marks_total']}** (theirs)\n" if theirs else "\n"))

    add("## Evidence per check\n")
    header = "| Check | Standard | Mine | Theirs |" if theirs else "| Check | Standard | Present | Where |"
    add(header)
    add("|---|---|---|---|" if theirs else "|---|---|---|---|")
    for signal in mine["signals"]:
        row_a = "yes" if signal["present"] else "**no**"
        if theirs:
            other = next(s for s in theirs["signals"] if s["key"] == signal["key"])
            row_b = "yes" if other["present"] else "**no**"
            add(f"| {signal['label']} | {signal['standard']} | {row_a} | {row_b} |")
        else:
            where = ", ".join(signal["evidence"][:3]) or "—"
            add(f"| {signal['label']} | {signal['standard']} | {row_a} | `{where}` |")
    add("")

    if theirs:
        only_mine = [s["label"] for s in mine["signals"]
                     if s["present"] and not next(o for o in theirs["signals"] if o["key"] == s["key"])["present"]]
        only_theirs = [s["label"] for s in theirs["signals"]
                       if s["present"] and not next(o for o in mine["signals"] if o["key"] == s["key"])["present"]]
        add("## What only one project has\n")
        add("**Only mine:** " + (", ".join(only_mine) if only_mine else "_nothing_"))
        add("")
        add("**Only theirs:** " + (", ".join(only_theirs) if only_theirs else "_nothing_"))
        add("")

    answer_key = ("\n---\n### How to read the score\n\n"
                  "The score is a **floor, not a mark**. It only counts detectable evidence on disk; "
                  "a human marker also judges demo quality, data realism and explanation. "
                  "Treat `no` as \"nothing found here — check by hand before claiming it\".\n")
    add(answer_key)
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser(description="Compare two CBDVentures codebases.")
    parser.add_argument("--mine", default=".", help="path to my project (default: .)")
    parser.add_argument("--theirs", help="path to the other project")
    parser.add_argument("--out", help="write the markdown report here")
    parser.add_argument("--json", dest="json_out", help="write raw profiles as JSON here")
    args = parser.parse_args()

    mine = profile(args.mine)
    theirs = profile(args.theirs) if args.theirs else None

    report = render(mine, theirs)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as handle:
            handle.write(report)
        print(f"Report written to {args.out}")
    else:
        print(report)

    if args.json_out:
        with open(args.json_out, "w", encoding="utf-8") as handle:
            json.dump({"mine": mine, "theirs": theirs}, handle, indent=2)
        print(f"Profiles written to {args.json_out}")


if __name__ == "__main__":
    main()
