# Code Review — Wild Dominion Qualifier Submissions

Ten examples of bad code drawn from the `qualifier_code` submissions, each with
a corrected version. For every example:

- `ex_N.[py|java]` — the bad snippet, copied verbatim, with a note on why it's bad.
- `ex_N_corrected.[py|java]` — a cleaner rewrite, with inline comments explaining
  why it's better.

| # | Language | Source submission | Problem category |
|---|----------|-------------------|------------------|
| 1 | Python | `postSec_006` (WrongFilename) | Noise comments that narrate syntax instead of intent (whole 1070-line file) |
| 2 | Python | `postSec_008` | Copy-paste bug — computer's stats are counted from the user's data |
| 3 | Python | `postSec_003` | Stray unconditional `break`, unreachable dead code, missing `f` prefix |
| 4 | Python | `postSec_005` | Redundant dead assignment + shadowing the `type` builtin |
| 5 | Python | `postSec_019` | Same method (`turn_2`) defined twice — first copy is dead |
| 6 | Python | `postSec_025` | Bare `except:` + indexing the grid before any bounds check (crash) |
| 7 | Java | `postSec_001` | No encapsulation — entire game state is global `static` fields |
| 8 | Java | `postSec_018` | Copy-pasted placement loop, magic-number divisors, RNG precedence bug |
| 9 | Java | `postSec_026` | Magic-number species codes needing comment crutches; duplicated block |
| 10 | Java | `postSec_002` | Misleading method name + "did not have time" confession for a required feature |

## Cross-cutting lessons

- **Comments should explain _why_, not _what_.** Examples 1, 9, and 10 all show
  comments standing in for readable code or missing behaviour.
- **Don't Repeat Yourself.** Duplication (2, 5, 8, 9) is where copy-paste bugs
  breed and maintenance costs multiply.
- **Fail loudly and specifically.** Bare `except:` (6) and unchecked indexing (6)
  hide real errors and cause runtime crashes the task explicitly forbids.
- **Name your constants and your state.** Magic numbers (8, 9) and global
  statics (7) obscure intent and make bugs impossible to localise.
- **Delete dead code.** Unreachable branches and duplicate definitions (3, 4, 5)
  mislead the next reader into editing code that never runs.

_All line numbers refer to the original submission files at review time._
