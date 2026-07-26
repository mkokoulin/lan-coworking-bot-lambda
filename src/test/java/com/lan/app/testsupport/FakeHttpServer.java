package com.lan.app.testsupport;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Minimal in-process HTTP server for exercising real outbound HTTP calls in tests without a
 * mocking framework (none is on the test classpath). Captures every request it receives and lets
 * a test script per-path-prefix responses.
 */
public class FakeHttpServer implements AutoCloseable {

    public record CapturedRequest(String method, String path, String query, String body) {}

    private final HttpServer server;
    private final List<CapturedRequest> requests = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> statusByPathPrefix = new ConcurrentHashMap<>();
    private final Map<String, String> bodyByPathPrefix = new ConcurrentHashMap<>();

    public FakeHttpServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.createContext("/", this::handle);
        server.start();
    }

    private void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        String body;
        try (var in = exchange.getRequestBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String path = exchange.getRequestURI().getPath();
        requests.add(new CapturedRequest(exchange.getRequestMethod(), path, exchange.getRequestURI().getQuery(), body));

        int status = 200;
        String respBody = "";
        for (var entry : statusByPathPrefix.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                status = entry.getValue();
                respBody = bodyByPathPrefix.getOrDefault(entry.getKey(), "");
                break;
            }
        }

        byte[] bytes = respBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    /** Makes any request whose path starts with {@code pathPrefix} get this canned response. */
    public void onPath(String pathPrefix, int status, String body) {
        statusByPathPrefix.put(pathPrefix, status);
        bodyByPathPrefix.put(pathPrefix, body);
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public List<CapturedRequest> requests() {
        return requests;
    }

    public List<CapturedRequest> requestsTo(String pathPrefix) {
        return requests.stream().filter(r -> r.path().startsWith(pathPrefix)).toList();
    }

    /** Polls until at least {@code count} requests have arrived — for asserting on fire-and-forget calls. */
    public void awaitAtLeast(int count, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (requests.size() < count && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
