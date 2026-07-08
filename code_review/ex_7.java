// ============================================================================
// BAD CODE EXAMPLE 7 — No encapsulation: the whole game is global static state
// Source: postSec_001/postSec_wildDominian/RunApp.java  (lines 4-28)
// ============================================================================

public class RunApp
{
    //declaring static (to be accessed from everywhere in code)
    static Scanner sc = new Scanner(System.in);

    static Scoreboard scoreboard = new Scoreboard();

    static String username;
    static int gridSize;

    static int userEagles;
    static int userFoxes;
    static int userRabbits;

    static int computerEagles;
    static int computerFoxes;
    static int computerRabbits;

    static int playerScore;
    static int computerScore;

    static String result;

    static char[][] playerGrid;
    static char[][] computerGrid;

// WHY THIS IS BAD:
// - Every piece of game state is a package-private `static` field, and the
//   comment proudly states the goal is "to be accessed from everywhere". That
//   is global mutable state — any method can silently change any field, so no
//   object owns its data and bugs are impossible to localise.
// - The user and computer are modelled as parallel loose variables
//   (userEagles/computerEagles ...) instead of objects, so the two players'
//   state can drift and every method needs to know all the globals.
// - It is untestable and un-reusable: you can never run two games, and there is
//   no object to construct in a unit test.
// - Ironically this submission HAS Animal/Eagle/Fox/Rabbit classes it barely
//   uses — the object model was available and ignored.
