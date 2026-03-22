import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.Executors;

/**
 * ChessServer.java
 * Plain Java HTTP server — no Maven, no frameworks.
 *
 * HOW TO RUN:
 *   javac *.java
 *   java ChessServer
 *   Open: http://localhost:8080
 */
public class ChessServer {

    static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT","8080"));

    public static void main(String[] args) throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress(PORT),0);
        ChessController ctrl=new ChessController();

        // API
        server.createContext("/api/", ex -> {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                cors(ex); ex.sendResponseHeaders(204,-1); return;
            }
            ctrl.handle(ex);
        });

        // Static files
        server.createContext("/", ex -> {
            String path=ex.getRequestURI().getPath();
            if (path.equals("/")) path="/index.html";
            File file=new File("."+path);
            if (!file.exists()||file.isDirectory()) file=new File("./index.html");
            if (!file.exists()) {
                String msg="File not found: "+path;
                ex.sendResponseHeaders(404,msg.length());
                ex.getResponseBody().write(msg.getBytes());
                ex.getResponseBody().close();
                return;
            }
            String ct=ct(file.getName());
            byte[] data=Files.readAllBytes(file.toPath());
            ex.getResponseHeaders().set("Content-Type",ct);
            cors(ex);
            ex.sendResponseHeaders(200,data.length);
            ex.getResponseBody().write(data);
            ex.getResponseBody().close();
        });

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("================================================");
        System.out.println("  Chess Engine running on port "+PORT);
        System.out.println("  Open: http://localhost:"+PORT);
        System.out.println("  ~1000 ELO  (Depth 3)");
        System.out.println("  ~1400 ELO  (Depth 4)");
        System.out.println("  Ctrl+C to stop");
        System.out.println("================================================");
    }

    static void cors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods","GET,POST,OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type");
    }

    static String ct(String name) {
        if (name.endsWith(".html")) return "text/html;charset=utf-8";
        if (name.endsWith(".css"))  return "text/css;charset=utf-8";
        if (name.endsWith(".js"))   return "application/javascript;charset=utf-8";
        return "application/octet-stream";
    }
}
