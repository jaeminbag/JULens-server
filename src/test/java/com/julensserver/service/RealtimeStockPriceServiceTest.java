package com.julensserver.service;

import com.julensserver.domain.Currency;
import com.julensserver.domain.Exchange;
import com.julensserver.domain.Stock;
import com.julensserver.dto.stock.RealtimeStockPriceResponse;
import com.julensserver.exception.BusinessException;
import com.julensserver.repository.StockRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RealtimeStockPriceServiceTest {

    @Test
    void 구독한_종목의_IEX_가격을_클라이언트에_전달한다() {
        FakeRealtimeStockPriceFeed feed = new FakeRealtimeStockPriceFeed();
        StockRepository repository = repositoryWithRequestedStocks();
        RealtimeStockPriceService service =
                new RealtimeStockPriceService(feed, repository);
        List<RealtimeStockPriceResponse> received = new ArrayList<>();

        RealtimeStockPriceService.Subscription subscription =
                service.subscribe(List.of("chow", "MU"), received::add);
        feed.emit(new RealtimeStockPrice(
                "CHOW",
                new BigDecimal("2.37"),
                new BigDecimal("100"),
                Instant.parse("2026-08-14T13:30:00Z"),
                "IEX"
        ));

        assertEquals(Set.of("CHOW", "MU"), feed.subscribed);
        assertEquals(1, received.size());
        assertEquals("CHOW", received.getFirst().ticker());
        assertEquals(new BigDecimal("2.37"), received.getFirst().price());
        assertEquals("IEX", received.getFirst().feed());

        subscription.close();
        assertEquals(Set.of("CHOW", "MU"), feed.unsubscribed);
    }

    @Test
    void 무료_IEX_한도인_30종목을_초과하면_거절한다() {
        FakeRealtimeStockPriceFeed feed = new FakeRealtimeStockPriceFeed();
        StockRepository repository = repositoryWithRequestedStocks();
        RealtimeStockPriceService service =
                new RealtimeStockPriceService(feed, repository);
        List<String> tickers = java.util.stream.IntStream.rangeClosed(1, 31)
                .mapToObj(index -> "S" + index)
                .toList();

        assertThrows(
                BusinessException.class,
                () -> service.subscribe(tickers, ignored -> { })
        );
    }

    private StockRepository repositoryWithRequestedStocks() {
        StockRepository repository = mock(StockRepository.class);
        when(repository.findAllByTickerIn(anyCollection())).thenAnswer(invocation -> {
            Collection<String> tickers = invocation.getArgument(0);
            return tickers.stream().map(this::stock).toList();
        });
        return repository;
    }

    private Stock stock(String ticker) {
        Stock stock = new Stock(
                ticker,
                ticker + " Company",
                ticker + " 컴퍼니",
                Exchange.NASDAQ,
                Currency.USD,
                null
        );
        stock.activate();
        return stock;
    }

    private static final class FakeRealtimeStockPriceFeed
            implements RealtimeStockPriceFeed {
        private Consumer<RealtimeStockPrice> consumer = ignored -> { };
        private Set<String> subscribed = Set.of();
        private Set<String> unsubscribed = Set.of();

        @Override
        public void setPriceConsumer(Consumer<RealtimeStockPrice> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void subscribe(Set<String> tickers) {
            subscribed = Set.copyOf(tickers);
        }

        @Override
        public void unsubscribe(Set<String> tickers) {
            unsubscribed = Set.copyOf(tickers);
        }

        private void emit(RealtimeStockPrice price) {
            consumer.accept(price);
        }
    }
}
