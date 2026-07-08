# ============================================================================
# CORRECTED EXAMPLE 2 — One helper removes the copy-paste (and the bug)
# ============================================================================

def count_species(grid):
    """Tally each species on a single {position: species} grid."""
    animals = list(grid.values())
    return {
        "Eagle": animals.count("Eagle"),
        "Fox": animals.count("Fox"),
        "Rabbit": animals.count("Rabbit"),
    }


# Call the SAME helper for each player. There is only one place the counting
# logic lives, so the two players cannot silently diverge.
user_counts = count_species(user_grid)
comp_counts = count_species(computer_grid)   # <-- reads the computer's grid


# WHY THIS IS BETTER:
# - The counting logic exists exactly once. It is impossible to "count the user
#   twice" because the grid to count is passed in as an argument.
# - DRY (Don't Repeat Yourself): duplicated blocks are where copy-paste bugs
#   breed. Removing the duplication removes the whole class of bug.
# - Returning a dict keeps related values together and makes call sites read
#   clearly: `user_counts["Fox"]` vs a loose pile of u_/c_ variables.
