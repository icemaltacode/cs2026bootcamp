// ============================================================================
// CORRECTED EXAMPLE 10 — Honest, parameterised method that does turn 3's work
// ============================================================================

    private void playGame() {
        runMovementTurn(2);
        runMovementTurn(3);   // same method, but it genuinely handles turn 3
    }

    // The name describes WHAT it does (run a movement turn), and the turn number
    // is a parameter, so turn 3 can branch to trigger the environmental event
    // instead of quietly reusing turn 2's behaviour.
    private void runMovementTurn(int turnNumber) {
        System.out.println("\n==========");
        System.out.println("| TURN " + turnNumber + " |");
        System.out.println("==========\n");

        moveAllAnimals();
        resolveInteractions();

        if (turnNumber == 3) {
            triggerEnvironmentalEvent();   // the previously-missing required feature
        }

        printSummary(turnNumber);
    }

// WHY THIS IS BETTER:
// - The method name (`runMovementTurn`) tells the truth for every turn it runs.
// - The turn number is data, not part of the name, so turn 3's extra rule lives
//   in a clear `if (turnNumber == 3)` branch instead of being silently skipped.
// - The environmental event is actually implemented rather than replaced by a
//   "did not have time" comment — meeting the task requirement.
// - If a feature genuinely cannot be finished, track it in an issue/TODO list,
//   not as a confession buried in a duplicated call.
