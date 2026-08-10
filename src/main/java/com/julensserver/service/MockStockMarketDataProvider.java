package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.StockMarketData;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;


@Component
@Profile("mock")
public class MockStockMarketDataProvider
        implements StockMarketDataProvider {

    @Override
    public StockMarketData getMarketData(Stock stock) {
        Objects.requireNonNull(
                stock,
                "시세를 조회할 종목은 null일 수 없습니다."
        );

        return new StockMarketData(
                new BigDecimal("25.50"),
                new BigDecimal("7.50"),
                3_000_000L,
                1_000_000L,
                new BigDecimal("76500000.00")
        );
    }
}