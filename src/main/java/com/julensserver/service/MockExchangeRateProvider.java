package com.julensserver.service;

import com.julensserver.dto.stock.ExchangeRateResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Profile("mock")
public class MockExchangeRateProvider implements ExchangeRateProvider {

    @Override
    public ExchangeRateResponse getUsdKrwRate() {
        return new ExchangeRateResponse(
                "USD",
                "KRW",
                new BigDecimal("1350.00"),
                LocalDate.of(2026, 8, 14)
        );
    }
}
