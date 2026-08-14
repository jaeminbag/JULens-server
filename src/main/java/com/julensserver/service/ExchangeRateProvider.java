package com.julensserver.service;

import com.julensserver.dto.stock.ExchangeRateResponse;

public interface ExchangeRateProvider {

    ExchangeRateResponse getUsdKrwRate();
}
