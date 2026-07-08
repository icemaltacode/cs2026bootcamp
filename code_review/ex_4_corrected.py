# ============================================================================
# CORRECTED EXAMPLE 4 — Store the value once; name things properly
# ============================================================================

EMPTY = 0   # named constant instead of a bare 0 sprinkled through the grid


def place_animal(grid, row, column, species):
    """Place `species` on `grid` at (row, column). Return True on success."""
    grid_size = len(grid)

    # Reject out-of-bounds and already-occupied cells up front (guard clauses).
    if not (0 <= row < grid_size and 0 <= column < grid_size):
        return False
    if grid[row][column] != EMPTY:
        return False

    grid[row][column] = species   # one assignment does the whole job
    return True


# WHY THIS IS BETTER:
# - The dead if/elif chain is gone: assigning `species` directly stores exactly
#   the same value the chain laboriously reproduced.
# - The player and computer paths become ONE reusable function (`grid` is a
#   parameter), so the duplicated block disappears too.
# - `species` does not shadow the `type` builtin; `column` is spelled correctly.
# - `EMPTY` names the "no animal" sentinel so the check reads as intent, not
#   as a magic 0.
