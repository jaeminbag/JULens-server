package com.julensserver.dto.stock;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateResponse(
        String baseCurrency,
        String quoteCurrency,
        BigDecimal rate,
        LocalDate asOf
) {
}
