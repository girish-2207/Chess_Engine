import java.util.*;

/**
 * Board.java
 * Row 0 = rank 8 (black back rank)
 * Row 7 = rank 1 (white back rank)
 * Positive = White, Negative = Black
 */
public class Board {
    public int[][] squares   = new int[8][8];
    public boolean whiteTurn = true;
    public int     epCol     = -1; // en passant column (-1 = none)

    // castling rights
    public boolean wKingMoved  = false;
    public boolean bKingMoved  = false;
    public boolean wRookAMoved = false; // a1
    public boolean wRookHMoved = false; // h1
    public boolean bRookAMoved = false; // a8
    public boolean bRookHMoved = false; // h8

    // move history
    public List<Move> history    = new ArrayList<>();
    public int        historyIdx = -1;

    public Board() { reset(); }

    public void reset() {
        for (int[] r : squares) Arrays.fill(r, Piece.EMPTY);

        // Black pieces row 0
        squares[0][0]=-Piece.ROOK; squares[0][1]=-Piece.KNIGHT; squares[0][2]=-Piece.BISHOP;
        squares[0][3]=-Piece.QUEEN; squares[0][4]=-Piece.KING;
        squares[0][5]=-Piece.BISHOP; squares[0][6]=-Piece.KNIGHT; squares[0][7]=-Piece.ROOK;
        for (int c=0;c<8;c++) squares[1][c]=-Piece.PAWN;

        // White pieces row 7
        squares[7][0]=Piece.ROOK; squares[7][1]=Piece.KNIGHT; squares[7][2]=Piece.BISHOP;
        squares[7][3]=Piece.QUEEN; squares[7][4]=Piece.KING;
        squares[7][5]=Piece.BISHOP; squares[7][6]=Piece.KNIGHT; squares[7][7]=Piece.ROOK;
        for (int c=0;c<8;c++) squares[6][c]=Piece.PAWN;

        whiteTurn=true; epCol=-1;
        wKingMoved=bKingMoved=false;
        wRookAMoved=wRookHMoved=false;
        bRookAMoved=bRookHMoved=false;
        history.clear(); historyIdx=-1;
    }

    public Board copy() {
        Board b=new Board();
        for (int r=0;r<8;r++) b.squares[r]=Arrays.copyOf(squares[r],8);
        b.whiteTurn=whiteTurn; b.epCol=epCol;
        b.wKingMoved=wKingMoved; b.bKingMoved=bKingMoved;
        b.wRookAMoved=wRookAMoved; b.wRookHMoved=wRookHMoved;
        b.bRookAMoved=bRookAMoved; b.bRookHMoved=bRookHMoved;
        return b;
    }

    public void makeMove(Move m) {
        // save state
        m.savedEpCol=epCol;
        m.savedWKMoved=wKingMoved; m.savedBKMoved=bKingMoved;
        m.savedWRAMoved=wRookAMoved; m.savedWRHMoved=wRookHMoved;
        m.savedBRAMoved=bRookAMoved; m.savedBRHMoved=bRookHMoved;

        epCol=-1;
        int piece=squares[m.fromRow][m.fromCol];

        // en passant: remove captured pawn
        if (m.isEnPassant) {
            int capRow = whiteTurn ? m.toRow+1 : m.toRow-1;
            squares[capRow][m.toCol]=Piece.EMPTY;
        }

        // castling: move rook
        if (m.isCastleKingside) {
            int row=whiteTurn?7:0;
            squares[row][5]=squares[row][7];
            squares[row][7]=Piece.EMPTY;
        }
        if (m.isCastleQueenside) {
            int row=whiteTurn?7:0;
            squares[row][3]=squares[row][0];
            squares[row][0]=Piece.EMPTY;
        }

        // move piece
        squares[m.toRow][m.toCol]=piece;
        squares[m.fromRow][m.fromCol]=Piece.EMPTY;

        // promotion
        if (m.isPromotion)
            squares[m.toRow][m.toCol]=whiteTurn?m.promotionPiece:-m.promotionPiece;

        // set ep flag for double pawn push
        if (Piece.type(piece)==Piece.PAWN && Math.abs(m.fromRow-m.toRow)==2)
            epCol=m.fromCol;

        // update castling rights
        if (Piece.type(piece)==Piece.KING) {
            if (whiteTurn) wKingMoved=true; else bKingMoved=true;
        }
        if (m.fromRow==7&&m.fromCol==0) wRookAMoved=true;
        if (m.fromRow==7&&m.fromCol==7) wRookHMoved=true;
        if (m.fromRow==0&&m.fromCol==0) bRookAMoved=true;
        if (m.fromRow==0&&m.fromCol==7) bRookHMoved=true;
        // rook captured = rights lost
        if (m.toRow==7&&m.toCol==0) wRookAMoved=true;
        if (m.toRow==7&&m.toCol==7) wRookHMoved=true;
        if (m.toRow==0&&m.toCol==0) bRookAMoved=true;
        if (m.toRow==0&&m.toCol==7) bRookHMoved=true;

        whiteTurn=!whiteTurn;
    }

    public void undoMove(Move m) {
        whiteTurn=!whiteTurn;
        int piece=squares[m.toRow][m.toCol];
        if (m.isPromotion) piece=whiteTurn?Piece.PAWN:-Piece.PAWN;

        squares[m.fromRow][m.fromCol]=piece;
        squares[m.toRow][m.toCol]=m.captured;

        if (m.isEnPassant) {
            int capRow=whiteTurn?m.toRow+1:m.toRow-1;
            squares[capRow][m.toCol]=whiteTurn?-Piece.PAWN:Piece.PAWN;
            squares[m.toRow][m.toCol]=Piece.EMPTY;
        }
        if (m.isCastleKingside) {
            int row=whiteTurn?7:0;
            squares[row][7]=squares[row][5]; squares[row][5]=Piece.EMPTY;
        }
        if (m.isCastleQueenside) {
            int row=whiteTurn?7:0;
            squares[row][0]=squares[row][3]; squares[row][3]=Piece.EMPTY;
        }

        epCol=m.savedEpCol;
        wKingMoved=m.savedWKMoved; bKingMoved=m.savedBKMoved;
        wRookAMoved=m.savedWRAMoved; wRookHMoved=m.savedWRHMoved;
        bRookAMoved=m.savedBRAMoved; bRookHMoved=m.savedBRHMoved;
    }

    public void recordMove(Move m) {
        while (history.size()>historyIdx+1) history.remove(history.size()-1);
        history.add(m); historyIdx++;
    }

    public boolean canUndo() { return historyIdx>=0; }
    public boolean canRedo() { return historyIdx<history.size()-1; }

    public String toJson() {
        StringBuilder sb=new StringBuilder("{\"squares\":[");
        for (int r=0;r<8;r++) {
            sb.append("[");
            for (int c=0;c<8;c++) { sb.append(squares[r][c]); if(c<7)sb.append(","); }
            sb.append(r<7 ? "]," : "]");
        }
        sb.append("],\"whiteTurn\":").append(whiteTurn)
          .append(",\"epCol\":").append(epCol)
          .append(",\"wKingMoved\":").append(wKingMoved)
          .append(",\"bKingMoved\":").append(bKingMoved)
          .append(",\"wRookAMoved\":").append(wRookAMoved)
          .append(",\"wRookHMoved\":").append(wRookHMoved)
          .append(",\"bRookAMoved\":").append(bRookAMoved)
          .append(",\"bRookHMoved\":").append(bRookHMoved)
          .append(",\"historyIdx\":").append(historyIdx)
          .append("}");
        return sb.toString();
    }
}
