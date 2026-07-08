// ============================================================================
// BAD CODE EXAMPLE 8 — Copy-pasted placement loop, magic numbers, precedence bug
// Source: postSec_018/postSec_wildDomination/Board.java  (lines 10-42)
// The same ~20-line loop is pasted again for foxes and rabbits (to line 88).
// ============================================================================

    Board (int size, boolean isPlayer) {
        this.size = size;
        this.numEagles = (int)Math.round((double)(size*size)/(double)36);
        this.numFoxes = (int)Math.round((double)(size*size)/(double)12);
        this.numRabbits = (int)Math.round((double)(size*size)/(double)8);

        animalBoard = new Animal[size][size];
        displayBoard = new String[size][size];

        int y;
        int x;
        if (isPlayer && numEagles!=0) {
            System.out.println("For eagles: \n");
        }
        for (int i=0; i<numEagles; i++) {
            if (isPlayer) {
                y = getYCoord();
                x = getXCoord();
            } else {
                y = (int)(Math.random()*size-1);
                x = (int)(Math.random()*size-1);
            }
            if (checkOccupied(y, x)) {
                if (isPlayer) {
                    System.out.println("Cell is already occupied");
                }
                i--;
            } else {
                animalBoard[y][x] = new Eagle();
                if (isPlayer) {
                    System.out.println("New eagle created at " + y + ", " + x + "\n");
                }
            }
        }
        // ... this entire loop is then pasted again for Fox, and again for Rabbit ...
    }

// WHY THIS IS BAD:
// - The ~20-line placement loop is copy-pasted three times, differing only in
//   the count, the `new Eagle()/Fox()/Rabbit()` call, and a log string. Any fix
//   (e.g. the bug below) must be made in three places.
// - `36`, `12`, `8` are unexplained magic numbers with no named constant.
// - `(int)(Math.random()*size-1)` is an operator-precedence BUG: it evaluates
//   as `(random*size) - 1`, which can produce -1 (an out-of-bounds index) and
//   never produces `size-1`. The intended range 0..size-1 is not what it gives.
