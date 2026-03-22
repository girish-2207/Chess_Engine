public class Piece {
    public static final int EMPTY  = 0;
    public static final int PAWN   = 1;
    public static final int KNIGHT = 2;
    public static final int BISHOP = 3;
    public static final int ROOK   = 4;
    public static final int QUEEN  = 5;
    public static final int KING   = 6;

    public static final int[] VALUE = {0, 100, 320, 330, 500, 900, 20000};

    public static int     type(int p)    { return Math.abs(p); }
    public static boolean isWhite(int p) { return p > 0; }
}
