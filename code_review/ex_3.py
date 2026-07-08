# ============================================================================
# BAD CODE EXAMPLE 3 — Stray break, unreachable code, and a broken f-string
# Source: postSec_003/postSec_wildDominion/main_app.py  (lines 532-549)
# Context: this sits inside `for turn in range(2, 4):`
# ============================================================================

        winner = ""
        if uf == 0 and ur == 0:
            print("\nComputer Wins!")
            winner = "Loss"
            break

        if cf == 0 and cr == 0:
            print("\nUser Wins!")
            winner = "Win"
            #winning melody
            win_tone()


        break

        if winner == "":
            winner = "Draw"
    print("The Final Result is a {winner}")

# WHY THIS IS BAD:
# - The unconditional `break` exits the `for turn in range(2, 4)` loop after a
#   SINGLE pass, so Turn 3 (and its required environmental event) never runs.
#   The loop is a lie — it looks like it iterates but it cannot.
# - The `if winner == "": winner = "Draw"` block sits AFTER that break, so it is
#   unreachable dead code — a draw can never be detected.
# - `"The Final Result is a {winner}"` is missing the `f` prefix, so it prints
#   the literal text `{winner}` instead of the value. A silent output bug.
