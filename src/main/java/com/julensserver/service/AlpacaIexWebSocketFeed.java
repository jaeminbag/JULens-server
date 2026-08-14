package com.julensserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Component
@Profile("real")
public class AlpacaIexWebSocketFeed
        implements RealtimeStockPriceFeed, SmartLifecycle {

    private static final Logger log =
            LoggerFactory.getLogger(AlpacaIexWebSocketFeed.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final long RECONNECT_DELAY_SECONDS = 5L;

    private final JsonMapper jsonMapper;
    private final HttpClient httpClient;
    private final URI streamUri;
    private final String apiKeyId;
    private final String secretKey;
    private final Set<String> desiredTickers = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService reconnectExecutor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "alpaca-iex-reconnect"
                );
                thread.setDaemon(true);
                return thread;
            });
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

    private volatile Consumer<RealtimeStockPrice> priceConsumer = price -> { };
    private volatile WebSocket webSocket;
    private volatile boolean running;
    private volatile boolean authenticated;

    public AlpacaIexWebSocketFeed(
            JsonMapper jsonMapper,
            @Value("${alpaca.iex-stream-url:"
                    + "wss://stream.data.alpaca.markets/v2/iex}")
            String streamUrl,
            @Value("${alpaca.api-key-id}") String apiKeyId,
            @Value("${alpaca.secret-key}") String secretKey
    ) {
        this.jsonMapper = jsonMapper;
        this.streamUri = URI.create(streamUrl);
        this.apiKeyId = requireCredential(apiKeyId, "Alpaca API Key ID");
        this.secretKey = requireCredential(secretKey, "Alpaca Secret Key");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    @Override
    public void setPriceConsumer(Consumer<RealtimeStockPrice> priceConsumer) {
        this.priceConsumer = priceConsumer == null ? price -> { } : priceConsumer;
    }

    @Override
    public void subscribe(Set<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return;
        }
        desiredTickers.addAll(tickers);
        if (authenticated) {
            sendSubscription("subscribe", tickers);
        }
    }

    @Override
    public void unsubscribe(Set<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return;
        }
        desiredTickers.removeAll(tickers);
        if (authenticated) {
            sendSubscription("unsubscribe", tickers);
        }
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        connect();
    }

    @Override
    public void stop() {
        running = false;
        authenticated = false;
        WebSocket current = webSocket;
        webSocket = null;
        if (current != null) {
            current.sendClose(
                    WebSocket.NORMAL_CLOSURE,
                    "JULens server stopping"
            );
        }
        reconnectExecutor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private void connect() {
        if (!running) {
            return;
        }
        httpClient.newWebSocketBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .buildAsync(streamUri, new AlpacaListener())
                .whenComplete((socket, error) -> {
                    if (error != null) {
                        log.warn("Alpaca IEX WebSocket connection failed", error);
                        scheduleReconnect();
                        return;
                    }
                    webSocket = socket;
                });
    }

    void acceptPayload(String payload) {
        try {
            JsonNode messages = jsonMapper.readTree(payload);
            if (!messages.isArray()) {
                return;
            }
            for (JsonNode message : messages) {
                handleMessage(message);
            }
        } catch (JacksonException exception) {
            log.warn("Invalid Alpaca IEX WebSocket payload", exception);
        }
    }

    private void handleMessage(JsonNode message) {
        String type = message.path("T").asString();
        if ("success".equals(type)) {
            handleSuccess(message.path("msg").asString());
            return;
        }
        if ("error".equals(type)) {
            log.warn(
                    "Alpaca IEX WebSocket error. code={}, message={}",
                    message.path("code").asInt(),
                    message.path("msg").asString()
            );
            return;
        }
        if (!"t".equals(type)) {
            return;
        }

        String ticker = message.path("S").asString()
                .trim()
                .toUpperCase(Locale.ROOT);
        JsonNode priceNode = message.get("p");
        JsonNode timestampNode = message.get("t");
        if (ticker.isBlank() || priceNode == null || timestampNode == null) {
            return;
        }

        BigDecimal price = priceNode.decimalValue();
        BigDecimal tradeSize = message.hasNonNull("s")
                ? message.get("s").decimalValue()
                : null;
        Instant timestamp;
        try {
            timestamp = Instant.parse(timestampNode.asString());
        } catch (RuntimeException exception) {
            return;
        }
        priceConsumer.accept(new RealtimeStockPrice(
                ticker,
                price,
                tradeSize,
                timestamp,
                "IEX"
        ));
    }

    private void handleSuccess(String message) {
        if ("connected".equals(message)) {
            send(Map.of(
                    "action", "auth",
                    "key", apiKeyId,
                    "secret", secretKey
            ));
            return;
        }
        if ("authenticated".equals(message)) {
            authenticated = true;
            reconnectScheduled.set(false);
            if (!desiredTickers.isEmpty()) {
                sendSubscription("subscribe", Set.copyOf(desiredTickers));
            }
            log.info("Alpaca IEX WebSocket authenticated");
        }
    }

    private void sendSubscription(String action, Set<String> tickers) {
        List<String> sortedTickers = new ArrayList<>(tickers);
        sortedTickers.sort(String::compareTo);
        send(Map.of(
                "action", action,
                "trades", sortedTickers
        ));
    }

    private void send(Map<String, ?> message) {
        WebSocket current = webSocket;
        if (current == null) {
            return;
        }
        try {
            current.sendText(jsonMapper.writeValueAsString(message), true);
        } catch (JacksonException exception) {
            log.warn("Alpaca IEX subscription message serialization failed", exception);
        }
    }

    private void disconnected() {
        authenticated = false;
        webSocket = null;
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        if (!running || !reconnectScheduled.compareAndSet(false, true)) {
            return;
        }
        reconnectExecutor.schedule(() -> {
            reconnectScheduled.set(false);
            connect();
        }, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private String requireCredential(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private final class AlpacaListener implements WebSocket.Listener {
        private final StringBuilder payload = new StringBuilder();

        @Override
        public void onOpen(WebSocket socket) {
            webSocket = socket;
            socket.request(1);
        }

        @Override
        public CompletionStage<?> onText(
                WebSocket socket,
                CharSequence data,
                boolean last
        ) {
            synchronized (payload) {
                payload.append(data);
                if (last) {
                    acceptPayload(payload.toString());
                    payload.setLength(0);
                }
            }
            socket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(
                WebSocket socket,
                int statusCode,
                String reason
        ) {
            log.warn(
                    "Alpaca IEX WebSocket closed. status={}, reason={}",
                    statusCode,
                    reason
            );
            disconnected();
            return null;
        }

        @Override
        public void onError(WebSocket socket, Throwable error) {
            log.warn("Alpaca IEX WebSocket failed", error);
            disconnected();
        }
    }
}
