package com.julensserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
@Profile("real")
public class AlpacaOvernightQuoteFeed
        implements RealtimeStockPriceFeed, SmartLifecycle {

    private static final Logger log =
            LoggerFactory.getLogger(AlpacaOvernightQuoteFeed.class);
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final LocalTime SESSION_OPEN = LocalTime.of(20, 0);
    private static final LocalTime SESSION_CLOSE = LocalTime.of(4, 0);

    private final AlpacaClient alpacaClient;
    private final long pollMillis;
    private final Set<String> desiredTickers = ConcurrentHashMap.newKeySet();
    private final Map<String, Instant> lastPublishedTimestamps =
            new ConcurrentHashMap<>();
    private final ScheduledExecutorService pollExecutor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "alpaca-overnight-quotes"
                );
                thread.setDaemon(true);
                return thread;
            });

    private volatile Consumer<RealtimeStockPrice> priceConsumer = price -> { };
    private volatile boolean running;

    public AlpacaOvernightQuoteFeed(
            AlpacaClient alpacaClient,
            @Value("${alpaca.overnight-poll-millis:5000}") long pollMillis
    ) {
        if (pollMillis < 1000) {
            throw new IllegalArgumentException(
                    "Overnight 호가 조회 간격은 1초 이상이어야 합니다."
            );
        }
        this.alpacaClient = alpacaClient;
        this.pollMillis = pollMillis;
    }

    @Override
    public void setPriceConsumer(Consumer<RealtimeStockPrice> priceConsumer) {
        this.priceConsumer = priceConsumer == null ? price -> { } : priceConsumer;
    }

    @Override
    public void subscribe(Set<String> tickers) {
        if (tickers != null) {
            desiredTickers.addAll(tickers);
        }
    }

    @Override
    public void unsubscribe(Set<String> tickers) {
        if (tickers != null) {
            desiredTickers.removeAll(tickers);
            tickers.forEach(lastPublishedTimestamps::remove);
        }
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        pollExecutor.scheduleWithFixedDelay(
                this::pollSafely,
                0,
                pollMillis,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void stop() {
        running = false;
        pollExecutor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private void pollSafely() {
        try {
            pollOnce(ZonedDateTime.now(NEW_YORK));
        } catch (RuntimeException exception) {
            log.warn("Alpaca overnight quote request failed", exception);
        }
    }

    void pollOnce(ZonedDateTime now) {
        if (!isOvernightSession(now) || desiredTickers.isEmpty()) {
            return;
        }

        Set<String> tickers = Set.copyOf(desiredTickers);
        AlpacaClient.AlpacaLatestQuotesResponse response =
                alpacaClient.getLatestOvernightQuotes(tickers);
        Map<String, AlpacaClient.AlpacaQuote> quotes = response.quotes();
        if (quotes == null || quotes.isEmpty()) {
            return;
        }

        Instant sessionStart = currentSessionStart(now);
        quotes.forEach((ticker, quote) -> publish(
                ticker,
                quote,
                tickers,
                sessionStart
        ));
    }

    static boolean isOvernightSession(ZonedDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        if (!time.isBefore(SESSION_OPEN)) {
            return day != DayOfWeek.FRIDAY
                    && day != DayOfWeek.SATURDAY;
        }
        if (time.isBefore(SESSION_CLOSE)) {
            return day != DayOfWeek.SATURDAY
                    && day != DayOfWeek.SUNDAY;
        }
        return false;
    }

    private static Instant currentSessionStart(ZonedDateTime now) {
        ZonedDateTime sessionDate = now.toLocalTime().isBefore(SESSION_CLOSE)
                ? now.minusDays(1)
                : now;
        return sessionDate.toLocalDate()
                .atTime(SESSION_OPEN)
                .atZone(NEW_YORK)
                .toInstant();
    }

    private void publish(
            String ticker,
            AlpacaClient.AlpacaQuote quote,
            Set<String> requestedTickers,
            Instant sessionStart
    ) {
        if (ticker == null
                || quote == null
                || quote.timestamp() == null
                || quote.timestamp().isBefore(sessionStart)) {
            return;
        }
        String normalizedTicker = ticker.trim().toUpperCase(Locale.ROOT);
        if (!requestedTickers.contains(normalizedTicker)) {
            return;
        }

        BigDecimal price = midpoint(quote.bidPrice(), quote.askPrice());
        if (price == null) {
            return;
        }
        Instant previousTimestamp =
                lastPublishedTimestamps.get(normalizedTicker);
        if (previousTimestamp != null
                && !quote.timestamp().isAfter(previousTimestamp)) {
            return;
        }
        lastPublishedTimestamps.put(normalizedTicker, quote.timestamp());
        priceConsumer.accept(new RealtimeStockPrice(
                normalizedTicker,
                price,
                null,
                quote.timestamp(),
                "OVERNIGHT"
        ));
    }

    static BigDecimal midpoint(BigDecimal bidPrice, BigDecimal askPrice) {
        boolean validBid = bidPrice != null && bidPrice.signum() > 0;
        boolean validAsk = askPrice != null && askPrice.signum() > 0;
        if (validBid && validAsk) {
            return bidPrice.add(askPrice).divide(BigDecimal.valueOf(2));
        }
        if (validBid) {
            return bidPrice;
        }
        return validAsk ? askPrice : null;
    }
}
