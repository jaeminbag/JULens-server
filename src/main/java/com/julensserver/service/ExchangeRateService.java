package com.julensserver.service;

import com.julensserver.dto.stock.ExchangeRateResponse;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateService {

    private final ExchangeRateProvider exchangeRateProvider;

    public ExchangeRateService(ExchangeRateProvider exchangeRateProvider) {
        this.exchangeRateProvider = exchangeRateProvider;
    }

    public ExchangeRateResponse getUsdKrwRate() {
        return exchangeRateProvider.getUsdKrwRate();
    }
}
