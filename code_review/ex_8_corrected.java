// ============================================================================
// CORRECTED EXAMPLE 8 — One reusable loop, named constants, correct RNG
// ============================================================================

    // Named constants explain WHY the divisors are what they are.
    private static final int EAGLE_DENSITY = 36;   // E = N*N / 36
    private static final int FOX_DENSITY   = 12;   // F = N*N / 12
    private static final int RABBIT_DENSITY = 8;   // R = N*N / 8

    private final Random random = new Random();

    Board(int size, boolean isPlayer) {
        this.size = size;
        animalBoard = new Animal[size][size];
        displayBoard = new String[size][size];

        // The placement loop exists ONCE; a Supplier<Animal> chooses the species.
        placeAnimals("eagle", roundCount(size, EAGLE_DENSITY),  Eagle::new,  isPlayer);
        placeAnimals("fox",   roundCount(size, FOX_DENSITY),    Fox::new,    isPlayer);
        placeAnimals("rabbit", roundCount(size, RABBIT_DENSITY), Rabbit::new, isPlayer);
    }

    private static int roundCount(int size, int density) {
        return (int) Math.round((double) (size * size) / density);
    }

    private void placeAnimals(String name, int count,
                              Supplier<Animal> factory, boolean isPlayer) {
        for (int placed = 0; placed < count; ) {
            int y, x;
            if (isPlayer) {
                y = getYCoord();
                x = getXCoord();
            } else {
                // nextInt(size) yields exactly 0..size-1 — no precedence bug.
                y = random.nextInt(size);
                x = random.nextInt(size);
            }

            if (checkOccupied(y, x)) {
                if (isPlayer) System.out.println("Cell is already occupied");
                continue;                       // retry without incrementing
            }
            animalBoard[y][x] = factory.get();  // new Eagle()/Fox()/Rabbit()
            if (isPlayer) {
                System.out.println("New " + name + " created at " + y + ", " + x);
            }
            placed++;
        }
    }

// WHY THIS IS BETTER:
// - The placement logic lives in ONE method; a fix or feature change happens
//   once, not three times.
// - `EAGLE_DENSITY` etc. name the formula divisors, so the intent is readable
//   and the numbers are changeable in one spot.
// - `random.nextInt(size)` returns a correct 0..size-1 index, fixing the
//   precedence bug that could produce -1.
