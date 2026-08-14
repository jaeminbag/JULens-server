package com.julensserver.service;

import java.util.Set;
import java.util.function.Consumer;

public interface RealtimeStockPriceFeed {

    void setPriceConsumer(Consumer<RealtimeStockPrice> priceConsumer);

    void subscribe(Set<String> tickers);

    void unsubscribe(Set<String> tickers);
}
