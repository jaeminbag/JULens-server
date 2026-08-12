package com.julensserver.service;

import com.julensserver.domain.Currency;
import com.julensserver.domain.Exchange;

import java.util.Locale;
import java.util.Objects;

public record StockSymbolData(
        String ticker,
        String companyName,
        Exchange exchange,
        Currency currency
) {
    public StockSymbolData {
        if (ticker == null || ticker.isBlank()
                || companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException(
                    "티커와 종목명은 비어 있을 수 없습니다."
            );
        }

        ticker = ticker.trim().toUpperCase(Locale.ROOT);
        companyName = companyName.trim();
        exchange = Objects.requireNonNull(exchange);
        currency = Objects.requireNonNull(currency);
    }
}
