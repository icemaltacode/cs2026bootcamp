# ============================================================================
# BAD CODE EXAMPLE 6 — Bare `except:` plus indexing before the bounds check
# Source: postSec_025/postSec_wildDominion/main_app.py  (lines 140-156)
# ============================================================================

def user_move(grid, turn_events):
    try:
        coords = input("Select animal (x,y): ")
        r1, c1 = coords.split(",")
        r1 = int(r1.strip())
        c1 = int(c1.strip())
        #propts user to enter coordinates
    except:
        print("***Invalid format***")
        return
    #vaildates for invalid format

    if grid[r1][c1] == "-":
        print("No animal at selected position.")
        return

    animal = grid[r1][c1]

# WHY THIS IS BAD:
# - Bare `except:` catches EVERYTHING — including KeyboardInterrupt (Ctrl-C) and
#   real programming errors — and mislabels them all "***Invalid format***",
#   hiding the true fault and making the program impossible to interrupt.
# - There is NO bounds check before `grid[r1][c1]`. A validly-formatted but
#   out-of-range input like `99,99` sails past the try/except and throws an
#   uncaught IndexError, crashing the game (violates the "avoid runtime errors"
#   rule in the task).
# - The comments are misspelled ("propts", "vaildates") and add nothing.
