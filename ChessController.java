import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ChessController {
    private Board board = new Board();

    public void handle(HttpExchange ex) throws IOException {
        String path=ex.getRequestURI().getPath();
        String body=new String(ex.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
        String resp;
        try {
            switch (path) {
                case "/api/new-game":    resp=newGame();         break;
                case "/api/legal-moves": resp=legalMoves(body); break;
                case "/api/player-move": resp=playerMove(body); break;
                case "/api/engine-move": resp=engineMove(body); break;
                case "/api/undo":        resp=undo();           break;
                case "/api/redo":        resp=redo();           break;
                default:                 resp=err("Unknown: "+path);
            }
        } catch(Exception e) { e.printStackTrace(); resp=err(e.getMessage()); }

        byte[] bytes=resp.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type","application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");
        ex.sendResponseHeaders(200,bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    String newGame() { board=new Board(); return ok("started"); }

    String legalMoves(String body) {
        int row=gi(body,"row"), col=gi(body,"col");
        StringBuilder sb=new StringBuilder("{\"moves\":[");
        boolean first=true;
        for (Move m:MoveGenerator.getLegalMoves(board)) {
            if (m.fromRow==row&&m.fromCol==col) {
                if (!first) sb.append(',');
                sb.append("{\"row\":").append(m.toRow).append(",\"col\":").append(m.toCol).append("}");
                first=false;
            }
        }
        return sb.append("]}").toString();
    }

    String playerMove(String body) {
        int fr=gi(body,"fromRow"),fc=gi(body,"fromCol");
        int tr=gi(body,"toRow"),  tc=gi(body,"toCol");
        Move chosen=null;
        for (Move m:MoveGenerator.getLegalMoves(board))
            if (m.fromRow==fr&&m.fromCol==fc&&m.toRow==tr&&m.toCol==tc) { chosen=m; break; }
        if (chosen==null) return err("Illegal move");
        chosen.san=NotationGenerator.toSAN(board,chosen);
        board.makeMove(chosen); board.recordMove(chosen);
        return ok(chosen.san);
    }

    String engineMove(String body) {
        int depth=gid(body,"depth",4);
        depth=Math.max(1,Math.min(5,depth));
        Move best=new Engine(depth).findBestMove(board);
        if (best==null) return err("No moves");
        best.san=NotationGenerator.toSAN(board,best);
        board.makeMove(best); board.recordMove(best);
        return ok(best.san);
    }

    String undo() {
        if (!board.canUndo()) return err("Nothing to undo");
        for (int i=0;i<2&&board.historyIdx>=0;i++)
            board.undoMove(board.history.get(board.historyIdx--));
        return ok("undone");
    }

    String redo() {
        if (!board.canRedo()) return err("Nothing to redo");
        for (int i=0;i<2&&board.historyIdx<board.history.size()-1;i++)
            board.makeMove(board.history.get(++board.historyIdx));
        return ok("redone");
    }

    String ok(String note) {
        List<Move> moves=MoveGenerator.getLegalMoves(board);
        boolean inCheck=MoveGenerator.isInCheck(board,board.whiteTurn);
        String status=moves.isEmpty()?(inCheck?"checkmate":"stalemate"):(inCheck?"check":"ok");

        StringBuilder hist=new StringBuilder("[");
        for (int i=0;i<=board.historyIdx;i++) {
            if(i>0) hist.append(',');
            hist.append('"').append(board.history.get(i).san).append('"');
        }
        hist.append(']');

        String lm="null";
        if (board.historyIdx>=0) {
            Move m=board.history.get(board.historyIdx);
            lm="{\"fromRow\":"+m.fromRow+",\"fromCol\":"+m.fromCol
              +",\"toRow\":"+m.toRow+",\"toCol\":"+m.toCol+"}";
        }

        return "{\"ok\":true,\"note\":\""+note+"\",\"status\":\""+status+"\""
             +",\"board\":"+board.toJson()
             +",\"history\":"+hist
             +",\"lastMove\":"+lm
             +",\"canUndo\":"+board.canUndo()
             +",\"canRedo\":"+board.canRedo()+"}";
    }

    String err(String msg) { return "{\"ok\":false,\"error\":\""+msg+"\"}"; }

    int gi(String json, String key) {
        String tok="\""+key+"\":";
        int i=json.indexOf(tok)+tok.length(), e=i;
        while(e<json.length()&&(Character.isDigit(json.charAt(e))||json.charAt(e)=='-')) e++;
        return Integer.parseInt(json.substring(i,e));
    }
    int gid(String json, String key, int def) {
        try { return gi(json,key); } catch(Exception e) { return def; }
    }
}
