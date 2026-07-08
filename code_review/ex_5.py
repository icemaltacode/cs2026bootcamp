# ============================================================================
# BAD CODE EXAMPLE 5 — Same method defined twice (the first is dead code)
# Source: postSec_019/postSec_wildDominion/main_app.py  (lines 303-326)
# ============================================================================

    def turn_2(self):
        while True:
            done = self.move_animal()
            if done == "stop":
                break

        self.predator_interaction()
        self.print_grids()

    def turn_2(self):
        print("+----------+")
        print("|  Turn 2  |")
        print("+----------+")
        while True:
            done = self.move_animal()
            if done == "stop":
                break

        print('''
------------------
  TURN 2 SUMMARY
------------------\n''')
        self.predator_interaction()
        self.print_grids()

# WHY THIS IS BAD:
# - `turn_2` is defined twice in the same class. Python silently keeps only the
#   LAST definition, so the first (with its own logic) is unreachable dead code.
# - Anyone editing the first version is editing a method that never runs — a
#   real time-sink and a source of "my change did nothing" confusion.
# - A linter (pyflakes/flake8 `F811 redefinition of unused 'turn_2'`) flags this
#   instantly; its presence shows the file was never linted or reviewed.
