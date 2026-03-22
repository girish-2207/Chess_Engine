public class Move {
    public int fromRow, fromCol, toRow, toCol;
    public int piece, captured;

    public boolean isEnPassant       = false;
    public boolean isCastleKingside  = false;
    public boolean isCastleQueenside = false;
    public boolean isPromotion       = false;
    public int     promotionPiece    = Piece.QUEEN;
    public String  san               = "";

    // saved state for undo
    public int     savedEpCol;
    public boolean savedWKMoved, savedBKMoved;
    public boolean savedWRAMoved, savedWRHMoved;
    public boolean savedBRAMoved, savedBRHMoved;

    public Move(int fr, int fc, int tr, int tc, int piece, int captured) {
        this.fromRow = fr; this.fromCol = fc;
        this.toRow   = tr; this.toCol   = tc;
        this.piece   = piece;
        this.captured= captured;
    }
}
