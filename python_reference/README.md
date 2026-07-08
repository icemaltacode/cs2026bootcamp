# Loan Desk — Python Reference Solution

Reference implementation for the **CODESPRINT 2026 Post-Secondary Bootcamp Task**
("Loan Desk"). It implements 100% of the required functionality and adds several
clearly-labelled extras. Standard library only; runs on Python 3.9+.

## Files

| File                      | Purpose                                                        |
| ------------------------- | ------------------------------------------------------------- |
| `loansdesk_reference.py`  | The program.                                                  |
| `catalogue.json`          | Starter catalogue (8 items — one of every type, some on loan).|

## Running

```bash
python3 loansdesk_reference.py                 # uses ./catalogue.json
python3 loansdesk_reference.py my_catalogue.json   # or any file you choose
```

The program loads on start-up and shows a menu. **It only stops via the Exit
option.** Menu input is case-insensitive and accepts either the number or a
keyword (e.g. `2` or `add`).

### Interface

The catalogue, menu and statistics are drawn in framed, box-drawing panels with
aligned columns; availability shows as a green `● Available` / yellow
`○ On loan` glyph, and status messages use `✓` / `✗` / `·` markers. Colour is
enabled only for a live terminal — it switches off automatically when output is
piped/redirected or the `NO_COLOR` environment variable is set, so the program
stays clean, aligned plain text when run non-interactively (e.g. for grading).

## How it maps to the task requirements

| # | Requirement                     | Where                                                              |
|---|---------------------------------|-------------------------------------------------------------------|
| 1 | Init & loading, main menu       | `Catalogue.load`, `LoanDeskApp.run` — missing/bad file → empty, no crash |
| 2 | Item types + extra fields       | `Item` base + `Laptop` / `Camera` / `RoboticsKit` subclasses      |
| 3 | View catalogue                  | `render_catalogue` — framed, aligned, adaptive widths, coloured availability |
| 4 | Add an item                     | `LoanDeskApp.add` + per-type builders; auto ID; starts Available  |
| 5 | Borrow / return                 | `LoanDeskApp.borrow_return` — toggles by ID, confirms, handles missing ID |
| 6 | Save                            | `Catalogue.save` — writes type tag + nested lens list             |
| 7 | Check routines (no crashes)     | validated-input layer + `try/except` around all file/JSON I/O     |
| 8 | Modular programming             | classes + functions, nested data structures, comments, PEP 8      |

**Round-trip rule (#6):** serialisation is polymorphic — every item builds its
own dict via `to_dict()` and is rebuilt via `from_dict()`. Save → load → save is
byte-stable (verified).

## Design highlights (why it's written this way)

- **Self-registering type system.** Each `Item` subclass declares a `type_tag`
  and `display_name`; `__init_subclass__` registers it. The loader, the add-item
  menu, and type dispatch all read that one registry — there is no hard-coded
  list of types anywhere. Adding a fourth item type is a ~15-line change.
- **Defensive I/O.** A missing/corrupt file degrades to an empty catalogue; a
  single malformed record is *skipped* (with a warning) rather than aborting the
  whole load; a duplicate ID in the file is skipped too.
- **Atomic save + backup.** Saving writes a temp file, `fsync`s, then
  `os.replace()`s over the target, after copying the old file to `<name>.bak`.
  A crash mid-write can never truncate the catalogue.
- **One validated-input layer.** Every prompt loops until the value is
  acceptable; `Ctrl-D`/`Ctrl-C` become a clean exit, not a traceback.

## Extra features (each worth +2 in the rubric)

1. **Search** — filter by name, type, or availability (`available` / `on loan`).
2. **Statistics** — totals plus a per-type breakdown.
3. **Unsaved-changes guard** — Exit warns and offers to save if there are
   pending changes.
4. **Command-line catalogue path** — run against any file, not just the default.
5. **Robust persistence** — atomic save with `.bak` backup, and skip-bad-record
   loading (beyond the required "don't crash").

All extras sit *on top of* the core rules and never alter required behaviour.

## Notes for markers

- **Name validation:** the task requires only that names be non-empty, which is
  enforced. As an extra, duplicate names are rejected (case-insensitively). Any
  characters are allowed, so real names like `micro:bit Go Bundle` are accepted.
- **Robotics type tag:** items are saved with the tag `"robotics kit"` (matching
  the official starter catalogue). For robustness the loader also accepts the
  short tag `"robotics"`, so catalogues written either way load correctly.
- **Starter catalogue:** `catalogue.json` is the official file from
  `https://codesprintmt.s3.us-east-1.amazonaws.com/catalogue.json`.
- This solution intentionally exceeds a one-hour answer to serve as a *model*.
  A submission does **not** need this much machinery to score full marks — the
  rubric rewards correct behaviour, clear structure, and good input handling,
  all of which a simpler solution can achieve.
```

## Verified behaviour

Exercised end-to-end: load → view → add (each type, with lenses) → borrow/return
→ search → statistics → save → reload. Confirmed: case-insensitive menus, zero/
negative/non-numeric rejection, corrupt & missing files handled gracefully,
individual bad records skipped, and a byte-stable save→load→save round trip.
