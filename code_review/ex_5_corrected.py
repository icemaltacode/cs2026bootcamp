# ============================================================================
# CORRECTED EXAMPLE 5 — One method, parameterised so it is not copy-pasted
# ============================================================================

class Game:

    # A single method handles any movement turn. The near-identical turn_2 /
    # turn_3 blocks collapse into one, driven by the turn number.
    def run_movement_turn(self, turn_number):
        print("+----------+")
        print(f"|  Turn {turn_number}  |")
        print("+----------+")

        while True:
            if self.move_animal() == "stop":
                break

        print(f"""
------------------
  TURN {turn_number} SUMMARY
------------------
""")
        self.predator_interaction()
        self.print_grids()


# WHY THIS IS BETTER:
# - There is exactly ONE definition, so nothing is silently shadowed and dead.
# - Because the turn number is a parameter, Turn 2 and Turn 3 share the code
#   instead of being two pasted copies that can drift apart.
# - Running `flake8`/`pyflakes` now passes; duplicate-definition bugs are caught
#   before they ship, not during grading.
