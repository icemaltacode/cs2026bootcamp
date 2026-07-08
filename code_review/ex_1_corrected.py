# ============================================================================
# CORRECTED EXAMPLE 1 — Let clear code speak for itself
# ============================================================================

def count_animals(animals):
    """Return a {species: count} tally for a list of animal records.

    A docstring like this documents the contract (what goes in, what comes
    out) in ONE place — far more useful than a comment on every line.
    """
    counts = {"Eagles": 0, "Foxes": 0, "Rabbits": 0}

    for animal in animals:
        counts[animal["animal"]] += 1

    return counts


# WHY THIS IS BETTER:
# - No comment restates syntax. The function/variable names ("count_animals",
#   "counts") already tell the reader what is happening.
# - A single docstring captures intent and the return shape — the one piece of
#   information a caller actually needs and cannot infer at a glance.
# - The code is now ~6 lines instead of ~20, so the real logic is visible at
#   once and there are no stale/wrong comments to maintain.
# - Rule of thumb: comment the WHY (a surprising rule, a workaround, a formula),
#   never the WHAT that the code already states.
