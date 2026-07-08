// ============================================================================
// CORRECTED EXAMPLE 7 — Give the state an owner: encapsulate it in objects
// ============================================================================

// A Player owns its own grid and animal counts. Fields are private, so the
// only way to change them is through the class's own methods (encapsulation).
public class Player {
    private final String name;
    private final char[][] grid;
    private int eagles;
    private int foxes;
    private int rabbits;

    public Player(String name, int gridSize) {
        this.name = name;
        this.grid = new char[gridSize][gridSize];
    }

    public int score() {
        return foxes * 2 + rabbits * 1;   // eagles are worth 0
    }
    // ... controlled getters/mutators (placeAnimal, moveAnimal, ...) go here ...
}

// The Game instance owns the two players and the Scanner. Nothing is static, so
// state is passed explicitly and two games could run side by side.
public class Game {
    private final Player user;
    private final Player computer;
    private final Scanner scanner = new Scanner(System.in);

    public Game(String username, int gridSize) {
        this.user = new Player(username, gridSize);
        this.computer = new Player("Computer", gridSize);
    }
}

// WHY THIS IS BETTER:
// - `private` fields mean each object is the single source of truth for its own
//   data; a bug in the user's count can only originate in Player code.
// - The user and computer are two instances of ONE class, so their behaviour is
//   guaranteed identical and cannot drift.
// - No global static state: the Game is constructed, testable, and repeatable.
// - This is exactly what the task's "modular / OO programming" criterion rewards.
