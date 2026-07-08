// ============================================================================
// BAD CODE EXAMPLE 10 — Misleading method name + a "confession" comment for a
//                       required feature that was never implemented
// Source: postSec_002/postSec_wildDominion/Game.java  (lines 86-96)
// ============================================================================

        System.out.println("\n==========");
        System.out.println("| TURN 2 |");
        System.out.println("==========\n");

        turn2();

        System.out.println("\n==========");
        System.out.println("| TURN 3 |");
        System.out.println("==========\n");

        turn2(); //Did not have time to do environment events

// WHY THIS IS BAD:
// - `turn2()` is called to run Turn 3, so the method NAME lies about what it
//   does. A name that hardcodes "2" cannot honestly run turn 3, and a reader
//   trusting the name will be misled.
// - The turn number is baked into the identifier instead of being a parameter,
//   which is exactly why turn 3 had to reuse the wrong-named method.
// - The trailing "//Did not have time to do environment events" is a confession
//   comment left in shipped code. Environmental events are a REQUIRED feature;
//   a comment is not a substitute for the behaviour, and it silently signals
//   the turn-3 logic is incomplete.
