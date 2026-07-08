# CODESPRINT 2026 — Post-Secondary Category · Bootcamp

Materials for the Post-Secondary **Bootcamp** session: the task sheet, two
reference solutions, and a set of code-review teaching examples.

The bootcamp task is **"Loan Desk"** — a small program that manages a campus
equipment-loan catalogue (laptops, cameras and robotics kits) that loads from a
file, lets staff view / add / lend items, and saves back so nothing is lost
between sessions. The full brief is in the task sheet.

## Contents

| Path | What it is |
| ---- | ---------- |
| `POSTSEC_TASK_2026_BOOTCAMP.pdf` | The task sheet handed to students — the "Loan Desk" brief, requirements, and assessment rubric. |
| `python_reference/` | Reference solution in **Python** (standard library only). Includes the program, the official starter `catalogue.json`, and its own README. |
| `java_reference/` | Reference solution in **Java** — a faithful port of the Python one. Gradle project (with wrapper) using **Gson** for JSON. Includes the program, `catalogue.json`, and its own README. |
| `code_review/` | Ten "spot the problem" examples (Python & Java) drawn from real qualifier submissions, each paired with a corrected version — teaching material for the *Discussion & Demos* segment. Has its own README. |

Each folder's `README.md` covers how to run it, how it maps to the rubric, and
the design decisions behind it.

## The two reference solutions

The Python and Java references are deliberately kept in step: same structure,
same behaviour, same on-screen look, and the **same file format** — they save
byte-for-byte identical catalogues and can load each other's output. Either one
is a complete model answer that hits 100% of the requirements plus a few
clearly-labelled extras.

- **Python:** `cd python_reference && python3 loansdesk_reference.py`
- **Java:** `cd java_reference && ./gradlew run` (Windows: `gradlew run`)

See each folder's README for full instructions and options.
