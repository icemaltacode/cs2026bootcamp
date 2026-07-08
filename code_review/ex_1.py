# ============================================================================
# BAD CODE EXAMPLE 1 — Noise comments that narrate syntax instead of intent
# Source: postSec_wildDominion_006_WrongFilename/main_app.py  (lines 59-78)
# ============================================================================

# Defines the function `count_animals`.
def count_animals(animal_list):
    # Starts a dictionary.
    counts = {
        # Provides a value inside the current data structure or call.
        "Eagles": 0,
        # Provides a value inside the current data structure or call.
        "Foxes": 0,
        # Runs this line as part of the program logic.
        "Rabbits": 0
    # Closes the current data structure or function call.
    }

    # Starts a loop through each item in a sequence.
    for animal in animal_list:
        # Stores a value in a variable.
        counts[animal["animal"]] += 1

    # Returns this value back to where the function was called.
    return counts

# WHY THIS IS BAD:
# - Every line has a comment that merely restates the Python syntax
#   ("Starts a dictionary", "Returns this value back to where the function was
#   called"). A reader already knows what `return` does.
# - This pattern is applied to the ENTIRE 1070-line file, roughly doubling its
#   length and burying the actual logic in noise.
# - The comments rot: some are flatly wrong elsewhere in the file (a plain
#   assignment is captioned "Closes the formatted message"; dict values are
#   captioned as function-call arguments).
# - Good comments explain WHY, not WHAT. Self-explanatory code needs none.
