# ============================================================================
# CORRECTED EXAMPLE 3 — Loop that actually loops; draw logic that runs
# ============================================================================

def run_turns(state):
    winner = ""

    for turn in range(2, 4):          # runs Turn 2 AND Turn 3
        run_turn_actions(state, turn)  # movement / interactions for this turn

        uf, ur, cf, cr = state.counts()

        if uf == 0 and ur == 0:
            print("\nComputer Wins!")
            winner = "Loss"
            break                     # early exit ONLY when a side is wiped out

        if cf == 0 and cr == 0:
            print("\nUser Wins!")
            winner = "Win"
            win_tone()                # play the winning melody
            break

    # Reached after the loop finishes normally OR after an early break.
    if winner == "":
        winner = "Draw"

    print(f"The Final Result is a {winner}")   # note the f-prefix
    return winner


# WHY THIS IS BETTER:
# - The only `break` statements are CONDITIONAL (a player was eliminated), so
#   the loop runs all three turns when nobody is wiped out — matching the rules.
# - The draw-detection runs AFTER the loop, where it is actually reachable, so
#   an even game is correctly reported as a Draw.
# - The `f` prefix is present, so the real winner is interpolated into the text.
# - Dead code (anything after an unconditional break/return) should be deleted,
#   not left to mislead the next reader.
