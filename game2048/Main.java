package game2048;

import ucb.util.CommandArgs;

import game2048.gui.Game;
import static game2048.Main.Side.*;

/** The main class for the 2048 game.
 *  @author Ajai Sharma
 */
public class Main {

    /** Size of the board: number of rows and of columns. */
    static final int SIZE = 4;
    
    /** Number of squares on the board. */
    static final int SQUARES = SIZE * SIZE;

    /** Symbolic names for the four sides of a board. */
    static enum Side { NORTH, EAST, SOUTH, WEST };

    /** The main program.  ARGS may contain the options --seed=NUM,
     *  (random seed); --log (record moves and random tiles
     *  selected.); --testing (take random tiles and moves from
     *  standard input); and --no-display. */
    public static void main(String... args) {
        CommandArgs options =
            new CommandArgs("--seed=(\\d+) --log --testing --no-display",
                            args);
        if (!options.ok()) {
            System.err.println("Usage: java game2048.Main [ --seed=NUM ] "
                               + "[ --log ] [ --testing ] [ --no-display ]");
            System.exit(1);
        }

        Main game = new Main(options);

        while (game.play()) {
            /* No action */
        }
        System.exit(0);
    }

    /** A new Main object using OPTIONS as options (as for main). */
    Main(CommandArgs options) {
        boolean log = options.contains("--log"),
            display = !options.contains("--no-display");
        long seed = !options.contains("--seed") ? 0 : options.getLong("--seed");
        _testing = options.contains("--testing");
        _game = new Game("2048", SIZE, seed, log, display, _testing);
    }

    /** Reset the score for the current game to 0 and clear the board. */
    void clear() {
        _score = 0;
        _count = 0;
        _game.clear();
        _game.setScore(_score, _maxScore);
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                _board[r][c] = 0;
            }
        }
    }

    /** Play one game of 2048, updating the maximum score. Return true
     *  iff play should continue with another game, or false to exit. */
    boolean play() {
        if (_count == 0) {
            setRandomPiece();
        }
        setRandomPiece();
        while (true) {
            if (gameOver()) {
                _maxScore = Math.max(_score, _maxScore);
                _game.setScore(_score, _maxScore);
                _game.endGame();
            }

        GetMove:
            while (true) {
                String key = _game.readKey();

                switch (key) {
                case "Up": case "Down": case "Left": case "Right":
                    if (!gameOver() && tiltBoard(keyToSide(key))) {
                        break GetMove;
                    }
                    break;
                case "Quit":
                    return false;
                case "New Game":
                    clear();
                    return true;
                default:
                    continue;
                }
            }
            return true;
        }
    }

    /** Returns true if any merges are possible. Assumes board is full.
     *  Helper function for gameOver. **/
    boolean mergePossible() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int value = _board[r][c];
                if (r - 1 >= 0 && _board[r - 1][c] == value) {
                    return true;
                }
                if (c - 1 >= 0 && _board[r][c - 1] == value) {
                    return true;
                }
                if (r + 1 < SIZE && _board[r + 1][c] == value) {
                    return true;
                }
                if (c + 1 < SIZE && _board[r][c + 1] == value) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Return true iff the current game is over (no more moves
     *  possible). */
    boolean gameOver() {
        if (_count == SQUARES && !mergePossible()) {
            return true;
        }

        return false;
    }

    /** Add a tile to a random, empty position, choosing a value (2 or
     *  4) at random.  Has no effect if the board is currently full. */

    void setRandomPiece() {
        if (_count == SQUARES) {
            return;
        }
        int[] place;
        while (true) {
            place = _game.getRandomTile();
            if (_board[place[1]][place[2]] == 0) {
                break;
            }
        }
        _board[place[1]][place[2]] = place[0];
        _game.addTile(place[0], place[1], place[2]);
        _count++;
    }

    /** Handles moving tiles; helper function for tiltBoard.
     *  Moves tile at R, C to RDEST, CDEST, in direction of SIDE,
     *  using BOARD as values. **/

    void moveWrapper(Side side, int r, int c,
            int rDest, int cDest, int[][] board) {
        int value = board[r][c];
        board[rDest][cDest] = value;
        board[r][c] = 0;
        _game.moveTile(value,
            tiltRow(side, r, c),
            tiltCol(side, r, c),
            tiltRow(side, rDest, cDest),
            tiltCol(side, rDest, cDest));
    }

    /** Handles merging tiles; helper function for tiltBoard.
     *  Merges tile at R, C to RDEST, CDEST, in direction of SIDE,
     *  using BOARD as values. **/
    void mergeWrapper(Side side, int r, int c,
             int rDest, int cDest, int[][] board) {
        int value = board[r][c];
        _score += 2 * value;
        _game.setScore(_score, _maxScore);
        _count--;
        board[r][c] = 0;
        board[rDest][cDest] = 2 * value;
        _game.mergeTile(value,
            2 * value,
            tiltRow(side, r, c),
            tiltCol(side, r, c),
            tiltRow(side, rDest, cDest),
            tiltCol(side, rDest, cDest));
    }


    /** Moves a column indexed by C towards the given SIDE, going
     *  down recursively from R0; helper function for tiltBoard.
     *  Returns true if any moves take place, false otherwise. **/
    boolean moveCol(Side side, int c, int r0, int[][] board) {
        if (r0 >= SIZE - 1) {
            return false;
        }
        boolean result = false;
        int value = board[r0][c];
        for (int r = r0 + 1; r < SIZE; r++) {
            if (board[r][c] == 0) {
                continue;
            } else if (value == 0) {
                result = true;
                moveWrapper(side, r, c, r0, c, board);
                value = board[r0][c];
                r = r0 + 1;
                continue;
            } else if (value == board[r][c]) {
                result = true;
                mergeWrapper(side, r, c, r0, c, board);
                break;
            } else {
                break;
            }
        }
        return moveCol(side, c, r0 + 1, board) || result;
    }



    /** Moves and merges tiles towards the given SIDE.
     *  Returns true if succeeds, else false. BOARD.
     *  Helper function for tiltBoard.  **/
    private boolean moveBoard(Side side, int[][] board) {
        boolean result = false;
        for (int c = 0; c < SIZE; c++) {
            result = moveCol(side, c, 0, board) || result;
        }
        return result;
    }

    /** Perform the result of tilting the board toward SIDE.
     *  Returns true iff the tilt changes the board. **/
    boolean tiltBoard(Side side) {
        /* Copies the board to a local array, turning it so that edge 
         * SIDE faces north.  Re-uses the same logic for all directions. */
        int[][] board = new int[SIZE][SIZE];
        boolean result = false;

        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                board[r][c] =
                    _board[tiltRow(side, r, c)][tiltCol(side, r, c)];
            }
        }

        result = moveBoard(side, board) || result;
        _game.displayMoves();

        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                _board[tiltRow(side, r, c)][tiltCol(side, r, c)]
                    = board[r][c];
            }
        }
        return result;
    }

    /** Return the row number on a playing board that corresponds to row R
     *  and column C of a board turned so that row 0 is in direction SIDE (as
     *  specified by the definitions of NORTH, EAST, etc.).  So, if SIDE
     *  is NORTH, then tiltRow simply returns R (since in that case, the
     *  board is not turned).  If SIDE is WEST, then column 0 of the tilted
     *  board corresponds to row SIZE - 1 of the untilted board, and
     *  tiltRow returns SIZE - 1 - C. */
    int tiltRow(Side side, int r, int c) {
        switch (side) {
        case NORTH:
            return r;
        case EAST:
            return c;
        case SOUTH:
            return SIZE - 1 - r;
        case WEST:
            return SIZE - 1 - c;
        default:
            throw new IllegalArgumentException("Unknown direction");
        }
    }

    /** Return the column number on a playing board that corresponds to row
     *  R and column C of a board turned so that row 0 is in direction SIDE
     *  (as specified by the definitions of NORTH, EAST, etc.). So, if SIDE
     *  is NORTH, then tiltCol simply returns C (since in that case, the
     *  board is not turned).  If SIDE is WEST, then row 0 of the tilted
     *  board corresponds to column 0 of the untilted board, and tiltCol
     *  returns R. */
    int tiltCol(Side side, int r, int c) {
        switch (side) {
        case NORTH:
            return c;
        case EAST:
            return SIZE - 1 - r;
        case SOUTH:
            return SIZE - 1 - c;
        case WEST:
            return r;
        default:
            throw new IllegalArgumentException("Unknown direction");
        }
    }

    /** Return the side indicated by KEY ("Up", "Down", "Left",
     *  or "Right"). */
    Side keyToSide(String key) {
        switch (key) {
        case "Up":
            return NORTH;
        case "Down":
            return SOUTH;
        case "Left":
            return WEST;
        case "Right":
            return EAST;
        default:
            throw new IllegalArgumentException("unknown key designation");
        }
    }

    /** Represents the board: _board[r][c] is the tile value at row R,
     *  column C, or 0 if there is no tile there. */
    private final int[][] _board = new int[SIZE][SIZE];

    /** True iff --testing option selected. */
    private boolean _testing;
    /** THe current input source and output sink. */
    private Game _game;
    /** The score of the current game, and the maximum final score
     *  over all games in this session. */
    private int _score, _maxScore;
    /** Number of tiles on the board. */
    private int _count;
}
