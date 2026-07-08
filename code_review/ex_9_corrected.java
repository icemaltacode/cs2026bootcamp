// ============================================================================
// CORRECTED EXAMPLE 9 — An enum makes the rules self-documenting; resolve once
// ============================================================================

// The species and their food chain live in ONE place. `eats()` reads like the
// rule sentence, so no "// Eagle vs Rabbit" comments are needed anywhere.
public enum Species {
    EAGLE, FOX, RABBIT;

    public boolean eats(Species prey) {
        return switch (this) {
            case EAGLE  -> prey == FOX || prey == RABBIT;
            case FOX    -> prey == RABBIT;
            case RABBIT -> false;
        };
    }
}

// Called ONCE per turn (turn 2 and turn 3 reuse it) instead of being pasted.
private void resolveInteractions() {
    for (int i = 0; i < gridSize; i++) {
        for (int j = 0; j < gridSize; j++) {
            Animal user = userAnimals[i][j];
            Animal computer = computerAnimals[i][j];
            if (user == null || computer == null) continue;   // empty cell

            if (user.getSpecies().eats(computer.getSpecies())) {
                recordEaten(computer.getSpecies());
                computerAnimals[i][j] = null;                 // eaten -> gone
            } else if (computer.getSpecies().eats(user.getSpecies())) {
                recordEaten(user.getSpecies());
                userAnimals[i][j] = null;
            }
            // same species -> nothing happens; no empty branch needed.
        }
    }
}

// WHY THIS IS BETTER:
// - `Species.EAGLE` / `.eats(...)` are self-explanatory, so the magic numbers
//   and their explanatory comments both disappear.
// - The food-chain rule is defined once on the enum; adding a species or
//   changing a rule is a single, safe edit.
// - `resolveInteractions()` is called once per turn, eliminating the duplicated
//   block. `null` for "empty cell" removes the -1 sentinel and its dead branch.
