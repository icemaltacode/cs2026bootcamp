# ============================================================================
# BAD CODE EXAMPLE 4 — Redundant dead assignment + shadowing a builtin
# Source: postSec_005/main.py  (lines 154-171)
# ============================================================================

    if user == "computer":

        if not (0 <= cell < gridSize and 0 <= coloumn < gridSize):
            return False

        if compGrid[cell][coloumn] != 0:
            return False

        compGrid[cell][coloumn] = type

        if type == "E":
            compGrid[cell][coloumn] = "E"
        elif type == "F":
            compGrid[cell][coloumn] = "F"
        elif type == "R":
            compGrid[cell][coloumn] = "R"

    return True

# WHY THIS IS BAD:
# - `compGrid[cell][coloumn] = type` already stores the value. The entire
#   if/elif chain then re-assigns the SAME value it was just given — pure dead
#   code that does nothing but pad the function and hide its intent.
# - This identical redundant block is also copy-pasted into the "player" branch,
#   doubling the noise.
# - `type` shadows the Python builtin `type()`, a bad habit that breaks any code
#   in the same scope that needs the real builtin.
# - `coloumn` is a misspelling of "column" repeated everywhere — small, but it
#   is a naming smell that spreads by copy-paste.
