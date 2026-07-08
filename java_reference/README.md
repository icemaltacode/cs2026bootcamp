# Loan Desk — Java Reference Solution

Reference implementation for the **CODESPRINT 2026 Post-Secondary Bootcamp Task**
("Loan Desk"). It implements 100% of the required functionality and adds several
clearly-labelled extras. Requires GSON.

## Files

| File                                       | Purpose                                            |
| ------------------------------------------ | -------------------------------------------------- |
| `src/main/java/LoansDeskReference.java`    | The program (one file, default package).           |
| `catalogue.json`                           | Official starter catalogue (8 items).              |
| `build.gradle` / `settings.gradle`         | Gradle build; pulls in Gson.                       |
| `gradlew`, `gradlew.bat`, `gradle/`        | Gradle wrapper — run without installing Gradle.    |

## Running

### Option 1 — Gradle wrapper (recommended)

The project ships a **Gradle wrapper**, so you do **not** need Gradle installed —
only a JDK (17 or newer). The wrapper downloads the correct Gradle version
(9.6.1) automatically the first time you run it. From inside the `java_reference`
folder:

**macOS / Linux**
```bash
./gradlew run                       # uses ./catalogue.json
./gradlew run --args="other.json"   # uses a file you choose
```
> If you get `permission denied` (the executable bit can be lost when the folder
> is copied or unzipped), run `chmod +x gradlew` once, or use `sh gradlew run`.

**Windows** (Command Prompt or PowerShell)
```bat
gradlew run
gradlew run --args="other.json"
```

The wrapper compiles, puts Gson on the classpath, and launches the program in one
step. It only stops via the **Exit** option. Menu input is case-insensitive and
accepts either the number or a keyword (e.g. `2` or `add`).

> First run needs internet (to fetch Gradle). On an offline/locked-down machine,
> run it once beforehand to warm the cache.

### Option 2 — installed Gradle

If Gradle is already installed, use it directly instead of the wrapper:
```bash
gradle run
gradle run --args="other.json"
```

### Option 3 — no Gradle at all

Because it's a single source file, you can run it with just a JDK and the Gson jar:
```bash
# download gson once
curl -L -o gson.jar https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar
java -cp gson.jar src/main/java/LoansDeskReference.java   # JDK 11+ source launcher
```

### Interface / colour

The catalogue, menu and statistics are drawn in framed, box-drawing panels with
aligned columns; availability shows as a green `● Available` / yellow
`○ On loan` glyph, and status messages use `✓` / `✗` / `·` markers.

When run directly, colour is emitted only for an interactive terminal and is
off when output is piped/redirected, so the program stays clean plain text for
grading. Under `./gradlew run` (or `gradle run`) neither Gradle nor the app can
detect the terminal (Gradle runs the app in its daemon and pipes the output), so
`build.gradle` sets `FORCE_COLOR=1` for the `run` task — the wrapper always
shows colour. To override:

- `NO_COLOR=1 ./gradlew run`   — force plain text (wins over `FORCE_COLOR`)
- `FORCE_COLOR=1 java ...`     — force colour when running the class directly

## How it maps to the task requirements

| # | Requirement                     | Where                                                              |
|---|---------------------------------|-------------------------------------------------------------------|
| 1 | Init & loading, main menu       | `Catalogue.load`, `LoanDeskApp.run` — missing/bad file → empty, no crash |
| 2 | Item types + extra fields       | `Item` base + `Laptop` / `Camera` / `RoboticsKit` subclasses      |
| 3 | View catalogue                  | `renderCatalogue` — framed, aligned, adaptive widths, coloured availability |
| 4 | Add an item                     | `LoanDeskApp.add` + per-type builders; auto ID; starts Available  |
| 5 | Borrow / return                 | `LoanDeskApp.borrowReturn` — toggles by ID, confirms, handles missing ID |
| 6 | Save                            | `Catalogue.save` — writes type tag + nested lens list             |
| 7 | Check routines (no crashes)     | validated-input layer + `try/catch` around all file/JSON I/O      |
| 8 | Modular programming             | classes + methods, nested data structures, comments, clear naming |

**Round-trip rule (#6):** each item builds its own `JsonObject` via `toJson()`
and is rebuilt via `Item.fromJson()`. Save → load → save is byte-stable.

## Design highlights

- **Gson tree model, not data binding.** Records are read as `JsonObject` /
  `JsonArray` and fields pulled out by hand — the direct analogue of the Python
  version reading plain dicts and lists, and the reason the two references
  behave identically. (Automatic POJO binding is deliberately avoided; it would
  hide the type-dispatch and validation that the task is really about.)
- **Data-driven type registry.** Each type is registered once with its tag,
  display name, a deserializer and an interactive builder. Loading, the add menu
  and type dispatch all read that registry — no hard-coded type list anywhere.
- **Defensive I/O.** Missing/corrupt file → empty catalogue; a single malformed
  record is skipped (with a warning); duplicate IDs in the file are skipped.
- **Atomic save + backup.** Writes a temp file then moves it over the target
  (atomic where the OS allows), after copying the old file to `<name>.bak`.
- **One validated-input layer.** Every prompt loops until the value is
  acceptable; end-of-input (Ctrl-D) exits cleanly instead of throwing.

## Extra features (each worth +2 in the rubric)

1. **Search** — filter by name, type, or availability (`available` / `on loan`).
2. **Statistics** — totals plus a per-type breakdown.
3. **Unsaved-changes guard** — Exit warns and offers to save pending changes.
4. **Command-line catalogue path** — run against any file (`--args="..."`).
5. **Robust persistence** — atomic save with a `.bak` backup, and skip-bad-record
   loading (beyond the required "don't crash").

## Notes for markers

- **Interchangeable with the Python reference.** Both save byte-for-byte
  identical files and load each other's output (verified). A catalogue produced
  by one works unchanged in the other.
- **Name validation:** the task requires only that names be non-empty, which is
  enforced. As an extra, duplicate names are rejected (case-insensitively). Any
  characters are allowed (e.g. `micro:bit Go Bundle`).
- **Robotics type tag:** items are saved with the tag `"robotics kit"` (matching
  the official starter catalogue); the short tag `"robotics"` is also accepted
  when loading.
- **Requires Java 17+** (compiles for 17; runs on any newer JDK). Only one
  third-party dependency, Gson, pulled in by Gradle.
- This solution intentionally exceeds a one-hour answer to serve as a *model*;
  a submission does not need this much machinery to score full marks.

## Verified behaviour

Exercised end-to-end: load → view → add (each type, with lenses) → borrow/return
→ search → statistics → save → reload. Confirmed: case-insensitive menus, zero/
negative/non-numeric rejection, corrupt & missing files handled gracefully,
individual bad records skipped, colour on/off detection, and a byte-stable
save→load→save round trip that matches the Python reference exactly.
