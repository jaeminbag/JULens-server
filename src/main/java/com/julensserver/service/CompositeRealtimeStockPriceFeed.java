package com.julensserver.service;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Consumer;

/** IEX 체결과 무료 Overnight 참고 호가를 하나의 SSE 소스로 합친다. */
@Component
@Primary
@Profile("real")
public class CompositeRealtimeStockPriceFeed implements RealtimeStockPriceFeed {

    private final AlpacaIexWebSocketFeed iexFeed;
    private final AlpacaOvernightQuoteFeed overnightFeed;

    public CompositeRealtimeStockPriceFeed(
            AlpacaIexWebSocketFeed iexFeed,
            AlpacaOvernightQuoteFeed overnightFeed
    ) {
        this.iexFeed = iexFeed;
        this.overnightFeed = overnightFeed;
    }

    @Override
    public void setPriceConsumer(Consumer<RealtimeStockPrice> priceConsumer) {
        iexFeed.setPriceConsumer(priceConsumer);
        overnightFeed.setPriceConsumer(priceConsumer);
    }

    @Override
    public void subscribe(Set<String> tickers) {
        iexFeed.subscribe(tickers);
        overnightFeed.subscribe(tickers);
    }

    @Override
    public void unsubscribe(Set<String> tickers) {
        iexFeed.unsubscribe(tickers);
        overnightFeed.unsubscribe(tickers);
    }
}
