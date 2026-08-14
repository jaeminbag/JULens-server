package com.julensserver.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Consumer;

@Component
@Profile("!real")
public class NoOpRealtimeStockPriceFeed implements RealtimeStockPriceFeed {

    @Override
    public void setPriceConsumer(Consumer<RealtimeStockPrice> priceConsumer) {
        // test/local 프로필에서는 외부 WebSocket에 연결하지 않는다.
    }

    @Override
    public void subscribe(Set<String> tickers) {
        // test/local 프로필에서는 외부 WebSocket에 연결하지 않는다.
    }

    @Override
    public void unsubscribe(Set<String> tickers) {
        // test/local 프로필에서는 외부 WebSocket에 연결하지 않는다.
    }
}
