// ============================================================================
// BAD CODE EXAMPLE 9 — Magic-number species codes needing comment crutches
// Source: postSec_026/postSec_wildDominion/RunApp.java  (lines 136-168)
// The whole ~33-line block is also duplicated verbatim for turn 3 (187-219).
// ============================================================================

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                Animal userAnimal = userAnimals[i][j];
                Animal computerAnimal = computerAnimals[i][j];

                // Compare the species, rabbit loses, fox beats rabbit and eagle beats both
                if (userAnimal.getSpecies() != -1 && computerAnimal.getSpecies() != -1) {
                    if (userAnimal.getSpecies() == computerAnimal.getSpecies()) {
                        // Both animals are of the same species, no action needed
                    } else if ((userAnimal.getSpecies() == 0 && computerAnimal.getSpecies() == 2) || // Eagle vs Rabbit
                               (userAnimal.getSpecies() == 0 && computerAnimal.getSpecies() == 1) || // Eagle vs Fox
                               (userAnimal.getSpecies() == 1 && computerAnimal.getSpecies() == 2)) { // Fox vs Rabbit
                        if (computerAnimal.getSpecies() == 2) {
                            computerRabbitsEaten++;
                        } else if (computerAnimal.getSpecies() == 1) {
                            computerFoxesEaten++;
                        }
                        computerAnimals[i][j] = new Animal(i, j, -1);
                    }
                }
            }
        }

// WHY THIS IS BAD:
// - Species are bare magic numbers (-1 empty, 0 eagle, 1 fox, 2 rabbit) with no
//   names, so every branch needs an inline "// Eagle vs Rabbit" comment just to
//   be legible. When code needs a comment to say what a literal MEANS, the
//   literal should have been a name.
// - The empty same-species `if` branch exists only to hold a comment.
// - The entire ~33-line resolution block is pasted a second time for turn 3, so
//   the predator rules live in two places that can drift apart.
