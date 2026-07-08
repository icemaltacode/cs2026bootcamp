# ============================================================================
# CORRECTED EXAMPLE 6 — Catch the specific error, validate before you index
# ============================================================================

def user_move(grid, turn_events):
    raw = input("Select animal (x,y): ")

    # Catch ONLY the error that parsing can actually raise. Anything else
    # (a real bug, Ctrl-C) is allowed to propagate instead of being hidden.
    try:
        row_text, col_text = raw.split(",")
        row = int(row_text.strip())
        col = int(col_text.strip())
    except ValueError:
        print("Invalid format — please enter two numbers like 3,4")
        return

    # Validate the range BEFORE using the values as indices, so an
    # out-of-bounds coordinate is rejected cleanly instead of crashing.
    size = len(grid)
    if not (0 <= row < size and 0 <= col < size):
        print("Position is outside the grid.")
        return

    if grid[row][col] == "-":
        print("No animal at selected position.")
        return

    animal = grid[row][col]


# WHY THIS IS BETTER:
# - `except ValueError` catches exactly the parse failure and nothing else, so
#   genuine bugs surface with a real traceback and Ctrl-C still works.
# - The bounds check runs BEFORE any indexing, so `99,99` is handled with a
#   friendly message instead of an IndexError crash.
# - Names (`row`, `col`, `raw`) say what they hold; the useless misspelled
#   comments are gone.
