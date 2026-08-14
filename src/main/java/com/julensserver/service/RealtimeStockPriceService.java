package com.julensserver.service;

import com.julensserver.dto.stock.RealtimeStockPriceResponse;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.StockRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Service
public class RealtimeStockPriceService {

    private static final int FREE_IEX_SYMBOL_LIMIT = 30;

    private final RealtimeStockPriceFeed priceFeed;
    private final StockRepository stockRepository;
    private final Object subscriptionLock = new Object();
    private final Map<String, Integer> referenceCounts = new ConcurrentHashMap<>();
    private final Map<String, RealtimeStockPriceResponse> latestPrices =
            new ConcurrentHashMap<>();
    private final List<ClientSubscription> subscriptions =
            new CopyOnWriteArrayList<>();

    public RealtimeStockPriceService(
            RealtimeStockPriceFeed priceFeed,
            StockRepository stockRepository
    ) {
        this.priceFeed = priceFeed;
        this.stockRepository = stockRepository;
        this.priceFeed.setPriceConsumer(this::publish);
    }

    public Subscription subscribe(
            List<String> requestedTickers,
            Consumer<RealtimeStockPriceResponse> consumer
    ) {
        Set<String> tickers = normalizeAndValidate(requestedTickers);
        ClientSubscription subscription = new ClientSubscription(
                tickers,
                consumer
        );
        Set<String> newlySubscribed = new LinkedHashSet<>();

        synchronized (subscriptionLock) {
            long additionalSymbols = tickers.stream()
                    .filter(ticker -> !referenceCounts.containsKey(ticker))
                    .count();
            if (referenceCounts.size() + additionalSymbols
                    > FREE_IEX_SYMBOL_LIMIT) {
                throw new BusinessException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "무료 IEX는 동시에 최대 30개 종목까지 구독할 수 있습니다."
                );
            }

            for (String ticker : tickers) {
                int previousCount = referenceCounts.getOrDefault(ticker, 0);
                referenceCounts.put(ticker, previousCount + 1);
                if (previousCount == 0) {
                    newlySubscribed.add(ticker);
                }
            }
            subscriptions.add(subscription);
        }

        if (!newlySubscribed.isEmpty()) {
            priceFeed.subscribe(newlySubscribed);
        }
        return subscription;
    }

    public List<RealtimeStockPriceResponse> getLatestPrices(
            List<String> requestedTickers
    ) {
        return normalizeAndValidate(requestedTickers).stream()
                .map(latestPrices::get)
                .filter(price -> price != null)
                .toList();
    }

    private Set<String> normalizeAndValidate(List<String> requestedTickers) {
        if (requestedTickers == null || requestedTickers.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Set<String> tickers = requestedTickers.stream()
                .filter(ticker -> ticker != null && !ticker.isBlank())
                .map(ticker -> ticker.trim().toUpperCase(Locale.ROOT))
                .collect(
                        LinkedHashSet::new,
                        LinkedHashSet::add,
                        LinkedHashSet::addAll
                );
        if (tickers.isEmpty()
                || tickers.size() > FREE_IEX_SYMBOL_LIMIT
                || tickers.stream().anyMatch(ticker ->
                !ticker.matches("[A-Z0-9.-]{1,20}"))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Set<String> existingTickers = new LinkedHashSet<>();
        stockRepository.findAllByTickerIn(tickers)
                .stream()
                .filter(stock -> stock.isActive())
                .forEach(stock -> existingTickers.add(
                        stock.getTicker().toUpperCase(Locale.ROOT)
                ));
        if (!existingTickers.containsAll(tickers)) {
            throw new BusinessException(ErrorCode.STOCK_NOT_FOUND);
        }
        return tickers;
    }

    private void publish(RealtimeStockPrice stockPrice) {
        if (stockPrice == null
                || stockPrice.price() == null
                || stockPrice.price().signum() <= 0) {
            return;
        }

        RealtimeStockPriceResponse response =
                RealtimeStockPriceResponse.from(stockPrice);
        latestPrices.put(response.ticker(), response);
        for (ClientSubscription subscription : subscriptions) {
            if (subscription.tickers.contains(response.ticker())) {
                try {
                    subscription.consumer.accept(response);
                } catch (RuntimeException exception) {
                    subscription.close();
                }
            }
        }
    }

    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    private final class ClientSubscription implements Subscription {
        private final Set<String> tickers;
        private final Consumer<RealtimeStockPriceResponse> consumer;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private ClientSubscription(
                Set<String> tickers,
                Consumer<RealtimeStockPriceResponse> consumer
        ) {
            this.tickers = Set.copyOf(tickers);
            this.consumer = consumer;
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }

            Set<String> noLongerUsed = new LinkedHashSet<>();
            synchronized (subscriptionLock) {
                subscriptions.remove(this);
                for (String ticker : tickers) {
                    int remaining = referenceCounts.getOrDefault(ticker, 1) - 1;
                    if (remaining <= 0) {
                        referenceCounts.remove(ticker);
                        noLongerUsed.add(ticker);
                    } else {
                        referenceCounts.put(ticker, remaining);
                    }
                }
            }
            if (!noLongerUsed.isEmpty()) {
                priceFeed.unsubscribe(noLongerUsed);
            }
        }
    }
}
