package in.gov.sih.sih26135.chaos;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test-only loopback HTTP server simulating external gateway outages (HTTP 500, 502, 503, 429, timeouts)
 * with zero external network egress.
 */
public class LocalMockHttpGatewayServer implements AutoCloseable {

  private final HttpServer server;
  private final int port;
  private final AtomicInteger responseCode = new AtomicInteger(200);
  private final AtomicLong delayMs = new AtomicLong(0);
  private volatile String responseBody = "{\"status\":\"ok\"}";
  private final AtomicInteger requestCount = new AtomicInteger(0);

  public LocalMockHttpGatewayServer() throws IOException {
    this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    this.port = this.server.getAddress().getPort();
    this.server.createContext("/", exchange -> {
      requestCount.incrementAndGet();
      long delay = delayMs.get();
      if (delay > 0) {
        try {
          Thread.sleep(delay);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      int code = responseCode.get();
      byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(code, bytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(bytes);
      }
    });
    this.server.start();
  }

  public int getPort() {
    return port;
  }

  public String getUrl() {
    return "http://127.0.0.1:" + port;
  }

  public void setResponseCode(int code) {
    this.responseCode.set(code);
  }

  public void setResponseBody(String body) {
    this.responseBody = body;
  }

  public void setDelayMs(long delay) {
    this.delayMs.set(delay);
  }

  public int getRequestCount() {
    return requestCount.get();
  }

  public void reset() {
    responseCode.set(200);
    delayMs.set(0);
    responseBody = "{\"status\":\"ok\"}";
    requestCount.set(0);
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
