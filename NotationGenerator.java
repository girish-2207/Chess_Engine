import java.util.List;

public class NotationGenerator {

    public static String toSAN(Board before, Move m) {
        int piece=before.squares[m.fromRow][m.fromCol];
        int type=Piece.type(piece);

        if (m.isCastleKingside)  return "O-O";
        if (m.isCastleQueenside) return "O-O-O";

        StringBuilder sb=new StringBuilder();
        if (type!=Piece.PAWN) sb.append(letter(type));
        if (type!=Piece.PAWN) sb.append(disambig(before,m,type));

        boolean cap=m.captured!=Piece.EMPTY||m.isEnPassant;
        if (cap) { if(type==Piece.PAWN) sb.append(file(m.fromCol)); sb.append('x'); }

        sb.append(file(m.toCol)).append(rank(m.toRow));
        if (m.isPromotion) sb.append("=Q");

        Board after=before.copy();
        after.makeMove(m);
        if (MoveGenerator.isInCheck(after,after.whiteTurn)) {
            sb.append(MoveGenerator.getLegalMoves(after).isEmpty()?'#':'+');
        }
        return sb.toString();
    }

    static String disambig(Board board, Move m, int type) {
        List<Move> all=MoveGenerator.getLegalMoves(board);
        boolean sameFile=false,sameRank=false,amb=false;
        for (Move o:all) {
            if (o.toRow==m.toRow&&o.toCol==m.toCol
                    &&Piece.type(board.squares[o.fromRow][o.fromCol])==type
                    &&(o.fromRow!=m.fromRow||o.fromCol!=m.fromCol)) {
                amb=true;
                if (o.fromCol==m.fromCol) sameFile=true;
                if (o.fromRow==m.fromRow) sameRank=true;
            }
        }
        if (!amb)      return "";
        if (!sameFile) return String.valueOf(file(m.fromCol));
        if (!sameRank) return String.valueOf(rank(m.fromRow));
        return ""+file(m.fromCol)+rank(m.fromRow);
    }

    static char letter(int t) {
        switch(t) {
            case Piece.KNIGHT:return'N'; case Piece.BISHOP:return'B';
            case Piece.ROOK:  return'R'; case Piece.QUEEN: return'Q';
            case Piece.KING:  return'K'; default:          return' ';
        }
    }
    static char   file(int c) { return (char)('a'+c); }
    static String rank(int r) { return String.valueOf(8-r); }
}
