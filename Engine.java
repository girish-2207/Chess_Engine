import java.util.List;

/**
 * Engine.java
 * Minimax + Alpha-Beta Pruning
 * Depth 3 = ~1000 ELO
 * Depth 4 = ~1400 ELO
 */
public class Engine {
    private static final int MATE_SCORE = 100000;

    private int  depth;
    private int  currentRootDepth;
    private Move bestMove;

    public Engine(int depth) { this.depth=depth; }

    public Move findBestMove(Board board) {
        bestMove=null;
        for (int d=1;d<=depth;d++) {
            currentRootDepth=d;
            search(board,d,Integer.MIN_VALUE,Integer.MAX_VALUE,board.whiteTurn,0);
        }
        return bestMove;
    }

    int search(Board board, int depth, int alpha, int beta, boolean maxing, int ply) {
        List<Move> moves=MoveGenerator.getLegalMoves(board);
        if (moves.isEmpty()) {
            if (MoveGenerator.isInCheck(board,board.whiteTurn))
                return maxing ? -MATE_SCORE + ply : MATE_SCORE - ply;
            return 0; // stalemate
        }

        if (depth==0) return Evaluator.evaluate(board);

        // captures first = better pruning
        moves.sort((a,b)->Integer.compare(Math.abs(b.captured),Math.abs(a.captured)));

        if (maxing) {
            int best=Integer.MIN_VALUE;
            for (Move m:moves) {
                board.makeMove(m);
                int score=search(board,depth-1,alpha,beta,false,ply+1);
                board.undoMove(m);
                if (score>best) { best=score; if(depth==currentRootDepth) bestMove=m; }
                alpha=Math.max(alpha,best);
                if (alpha>=beta) break;
            }
            return best;
        } else {
            int best=Integer.MAX_VALUE;
            for (Move m:moves) {
                board.makeMove(m);
                int score=search(board,depth-1,alpha,beta,true,ply+1);
                board.undoMove(m);
                if (score<best) { best=score; if(depth==currentRootDepth) bestMove=m; }
                beta=Math.min(beta,best);
                if (alpha>=beta) break;
            }
            return best;
        }
    }
}
