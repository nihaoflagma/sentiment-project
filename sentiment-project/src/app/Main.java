package app;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    static AtomicLong requestCount = new AtomicLong(0);

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/sentiment", new SentimentHandler());
        server.createContext("/metrics", new MetricsHandler());
        server.start();
        System.out.println("Server started on port " + port);
    }

    static class SentimentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            requestCount.incrementAndGet();
            URI req = exchange.getRequestURI();
            String query = req.getQuery();
            Map<String,String> params = queryToMap(query);
            String text = params.getOrDefault("text", "");
            String sentiment = analyzeMock(text);
            String json = String.format("{\"sentiment\":\"%s\",\"text\":\"%s\"}", sentiment, escapeJson(text));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            byte[] resp = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            OutputStream os = exchange.getResponseBody();
            os.write(resp);
            os.close();
        }
    }

    static class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = "# HELP app_requests_total Total number of requests\n"
                    + "# TYPE app_requests_total counter\n"
                    + "app_requests_total " + requestCount.get() + "\n";
            exchange.getResponseHeaders().add("Content-Type", "text/plain; version=0.0.4");
            byte[] resp = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            OutputStream os = exchange.getResponseBody();
            os.write(resp);
            os.close();
        }
    }

    static String analyzeMock(String text) {
        if (text == null || text.trim().isEmpty()) return "neutral";
        String t = text.toLowerCase();
        if (t.contains("good") || t.contains("happy") || t.contains("great")) return "positive";
        if (t.contains("bad") || t.contains("terrible") || t.contains("sad")) return "negative";
        return "neutral";
    }

    static Map<String,String> queryToMap(String query){
        Map<String,String> result = new HashMap<>();
        if(query == null) return result;
        for (String param : query.split("&")) {
            String[] pair = param.split("=",2);
            String key = decode(pair[0]);
            String value = pair.length>1 ? decode(pair[1]) : "";
            result.put(key, value);
        }
        return result;
    }

    static String decode(String s){
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    static String escapeJson(String s){
        return s.replace("\"","\\\"").replace("\n","\\n");
    }
}
