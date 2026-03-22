import java.util.*;

/**
 * MoveGenerator.java
 * Generates all legal chess moves.
 * Every rule: castling, en passant, promotion, check detection.
 */
public class MoveGenerator {

    // ---- public API ----

    public static List<Move> getLegalMoves(Board board) {
        List<Move> legal = new ArrayList<>();
        for (Move m : getPseudo(board)) {
            Board copy = board.copy();
            copy.makeMove(m);
            if (!isInCheck(copy, board.whiteTurn)) legal.add(m);
        }
        return legal;
    }

    public static boolean isInCheck(Board board, boolean white) {
        int king = white ? Piece.KING : -Piece.KING;
        for (int r=0;r<8;r++)
            for (int c=0;c<8;c++)
                if (board.squares[r][c]==king) return isAttacked(board,r,c,white);
        return false;
    }

    // ---- pseudo-legal move generation ----

    static List<Move> getPseudo(Board board) {
        List<Move> moves = new ArrayList<>();
        for (int r=0;r<8;r++) {
            for (int c=0;c<8;c++) {
                int p=board.squares[r][c];
                if (p==Piece.EMPTY) continue;
                if (Piece.isWhite(p)!=board.whiteTurn) continue;
                switch (Piece.type(p)) {
                    case Piece.PAWN:   pawnMoves  (board,r,c,moves); break;
                    case Piece.KNIGHT: knightMoves(board,r,c,moves); break;
                    case Piece.BISHOP: sliding(board,r,c,moves,false,true);  break;
                    case Piece.ROOK:   sliding(board,r,c,moves,true,false);  break;
                    case Piece.QUEEN:  sliding(board,r,c,moves,true,true);   break;
                    case Piece.KING:   kingMoves  (board,r,c,moves); break;
                }
            }
        }
        return moves;
    }

    // ---- pawn moves ----
    static void pawnMoves(Board board, int r, int c, List<Move> moves) {
        boolean white = board.squares[r][c]>0;
        int p         = board.squares[r][c];
        int dir       = white?-1:1;
        int startRow  = white?6:1;
        int promoRow  = white?0:7;
        int nr        = r+dir;

        // single push
        if (ok(nr,c) && board.squares[nr][c]==Piece.EMPTY) {
            if (nr==promoRow) addPromo(p,r,c,nr,c,Piece.EMPTY,moves);
            else              moves.add(new Move(r,c,nr,c,p,Piece.EMPTY));
            // double push
            int nr2=r+2*dir;
            if (r==startRow && board.squares[nr2][c]==Piece.EMPTY)
                moves.add(new Move(r,c,nr2,c,p,Piece.EMPTY));
        }

        // diagonal captures + en passant
        for (int dc : new int[]{-1,1}) {
            int nc=c+dc;
            if (!ok(nr,nc)) continue;
            int t=board.squares[nr][nc];

            // normal capture
            if (t!=Piece.EMPTY && Piece.isWhite(t)!=white) {
                if (nr==promoRow) addPromo(p,r,c,nr,nc,t,moves);
                else              moves.add(new Move(r,c,nr,nc,p,t));
            }

            // en passant: epCol is the column of pawn that just double-pushed
            if (board.epCol==nc && r==(white?3:4)) {
                Move m=new Move(r,c,nr,nc,p,Piece.EMPTY);
                m.isEnPassant=true;
                moves.add(m);
            }
        }
    }

    static void addPromo(int p, int fr, int fc, int tr, int tc, int cap, List<Move> moves) {
        Move m=new Move(fr,fc,tr,tc,p,cap);
        m.isPromotion=true; m.promotionPiece=Piece.QUEEN;
        moves.add(m);
    }

    // ---- knight moves ----
    static void knightMoves(Board board, int r, int c, List<Move> moves) {
        boolean white=board.squares[r][c]>0;
        int p=board.squares[r][c];
        for (int[] o:new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}}) {
            int nr=r+o[0],nc=c+o[1];
            if (!ok(nr,nc)) continue;
            int t=board.squares[nr][nc];
            if (t!=Piece.EMPTY && Piece.isWhite(t)==white) continue;
            moves.add(new Move(r,c,nr,nc,p,t));
        }
    }

    // ---- sliding moves (bishop/rook/queen) ----
    static void sliding(Board board, int r, int c, List<Move> moves, boolean straight, boolean diag) {
        boolean white=board.squares[r][c]>0;
        int p=board.squares[r][c];
        int[][] dirs=buildDirs(straight,diag);
        for (int[] d:dirs) {
            int nr=r+d[0],nc=c+d[1];
            while (ok(nr,nc)) {
                int t=board.squares[nr][nc];
                if (t==Piece.EMPTY) {
                    moves.add(new Move(r,c,nr,nc,p,Piece.EMPTY));
                } else {
                    if (Piece.isWhite(t)!=white) moves.add(new Move(r,c,nr,nc,p,t));
                    break;
                }
                nr+=d[0]; nc+=d[1];
            }
        }
    }

    static int[][] buildDirs(boolean s, boolean d) {
        if (s&&d) return new int[][]{{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}};
        if (s)    return new int[][]{{-1,0},{1,0},{0,-1},{0,1}};
        return          new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}};
    }

    // ---- king moves + castling ----
    static void kingMoves(Board board, int r, int c, List<Move> moves) {
        boolean white=board.squares[r][c]>0;
        int p=board.squares[r][c];

        // normal steps
        for (int[] d:new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}) {
            int nr=r+d[0],nc=c+d[1];
            if (!ok(nr,nc)) continue;
            int t=board.squares[nr][nc];
            if (t!=Piece.EMPTY && Piece.isWhite(t)==white) continue;
            moves.add(new Move(r,c,nr,nc,p,t));
        }

        // White castling
        if (white && !board.wKingMoved && r==7 && c==4) {
            // kingside: e1-f1-g1 must be empty and safe
            if (!board.wRookHMoved
                    && board.squares[7][5]==Piece.EMPTY
                    && board.squares[7][6]==Piece.EMPTY
                    && !isAttacked(board,7,4,true)
                    && !isAttacked(board,7,5,true)
                    && !isAttacked(board,7,6,true)) {
                Move m=new Move(7,4,7,6,p,Piece.EMPTY);
                m.isCastleKingside=true; moves.add(m);
            }
            // queenside: e1-d1-c1 must be safe, b1 just empty
            if (!board.wRookAMoved
                    && board.squares[7][1]==Piece.EMPTY
                    && board.squares[7][2]==Piece.EMPTY
                    && board.squares[7][3]==Piece.EMPTY
                    && !isAttacked(board,7,4,true)
                    && !isAttacked(board,7,3,true)
                    && !isAttacked(board,7,2,true)) {
                Move m=new Move(7,4,7,2,p,Piece.EMPTY);
                m.isCastleQueenside=true; moves.add(m);
            }
        }

        // Black castling
        if (!white && !board.bKingMoved && r==0 && c==4) {
            // kingside
            if (!board.bRookHMoved
                    && board.squares[0][5]==Piece.EMPTY
                    && board.squares[0][6]==Piece.EMPTY
                    && !isAttacked(board,0,4,false)
                    && !isAttacked(board,0,5,false)
                    && !isAttacked(board,0,6,false)) {
                Move m=new Move(0,4,0,6,p,Piece.EMPTY);
                m.isCastleKingside=true; moves.add(m);
            }
            // queenside
            if (!board.bRookAMoved
                    && board.squares[0][1]==Piece.EMPTY
                    && board.squares[0][2]==Piece.EMPTY
                    && board.squares[0][3]==Piece.EMPTY
                    && !isAttacked(board,0,4,false)
                    && !isAttacked(board,0,3,false)
                    && !isAttacked(board,0,2,false)) {
                Move m=new Move(0,4,0,2,p,Piece.EMPTY);
                m.isCastleQueenside=true; moves.add(m);
            }
        }
    }

    // ---- attack detection ----
    // Is square (r,c) attacked by the opponent of defendingWhite?
    public static boolean isAttacked(Board board, int r, int c, boolean defendingWhite) {
        // Knight attacks
        for (int[] o:new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}}) {
            int nr=r+o[0],nc=c+o[1];
            if (!ok(nr,nc)) continue;
            int p=board.squares[nr][nc];
            if (p!=Piece.EMPTY && Piece.type(p)==Piece.KNIGHT && Piece.isWhite(p)!=defendingWhite)
                return true;
        }

        // Pawn attacks
        // White pawns move UP (decreasing row), attack diagonally up
        // Black pawns move DOWN (increasing row), attack diagonally down
        // If defending white: enemy is black, black pawns attack from ABOVE (r-1)
        // If defending black: enemy is white, white pawns attack from BELOW (r+1)
        int pawnRow = defendingWhite ? r-1 : r+1;
        for (int dc:new int[]{-1,1}) {
            int nc=c+dc;
            if (!ok(pawnRow,nc)) continue;
            int p=board.squares[pawnRow][nc];
            if (p==Piece.EMPTY) continue;
            if (Piece.type(p)==Piece.PAWN && Piece.isWhite(p)!=defendingWhite) return true;
        }

        // Straight attacks (rook/queen)
        for (int[] d:new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
            int nr=r+d[0],nc=c+d[1];
            while (ok(nr,nc)) {
                int p=board.squares[nr][nc];
                if (p!=Piece.EMPTY) {
                    if (Piece.isWhite(p)!=defendingWhite) {
                        int t=Piece.type(p);
                        if (t==Piece.ROOK||t==Piece.QUEEN) return true;
                    }
                    break;
                }
                nr+=d[0]; nc+=d[1];
            }
        }

        // Diagonal attacks (bishop/queen)
        for (int[] d:new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}}) {
            int nr=r+d[0],nc=c+d[1];
            while (ok(nr,nc)) {
                int p=board.squares[nr][nc];
                if (p!=Piece.EMPTY) {
                    if (Piece.isWhite(p)!=defendingWhite) {
                        int t=Piece.type(p);
                        if (t==Piece.BISHOP||t==Piece.QUEEN) return true;
                    }
                    break;
                }
                nr+=d[0]; nc+=d[1];
            }
        }

        // King attacks
        for (int[] d:new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}) {
            int nr=r+d[0],nc=c+d[1];
            if (!ok(nr,nc)) continue;
            int p=board.squares[nr][nc];
            if (Piece.type(p)==Piece.KING && Piece.isWhite(p)!=defendingWhite) return true;
        }

        return false;
    }

    static boolean ok(int r, int c) { return r>=0&&r<8&&c>=0&&c<8; }
}
