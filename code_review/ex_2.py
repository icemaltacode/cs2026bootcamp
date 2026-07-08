# ============================================================================
# BAD CODE EXAMPLE 2 — Copy-paste bug: computer's stats read the USER's data
# Source: postSec_008/postSec_wildDominion/main_app.py  (lines 341-352)
# ============================================================================

    user_animals = list(user_grid.values())
    comp_animals = list(computer_grid.values())

    # User
    u_eagles = user_animals.count("Eagle")
    u_foxes = user_animals.count("Fox")
    u_rabbits = user_animals.count("Rabbit")

    # Computer
    c_eagles = user_animals.count("Eagle")
    c_foxes = user_animals.count("Fox")
    c_rabbits = user_animals.count("Rabbit")

# WHY THIS IS BAD:
# - `comp_animals` is built (line 2) but never used.
# - The computer's counts are copy-pasted from the USER block and still read
#   `user_animals`. So the computer's Eagle/Fox/Rabbit totals always MIRROR the
#   user's — a genuine correctness bug that corrupts the final score and the
#   win/lose decision, the whole point of the game.
# - The bug is invisible precisely because the three lines look "the same" as
#   the block above; copy-paste hides the one word that had to change.
# - A related twin bug lives at lines 367/373, where both columns are labelled
#   "Eagles:" so foxes get printed under an "Eagles" header.
