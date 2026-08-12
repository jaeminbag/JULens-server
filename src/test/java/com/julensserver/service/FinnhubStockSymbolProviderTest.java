package com.julensserver.service;

import com.julensserver.domain.Exchange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinnhubStockSymbolProviderTest {

    @Test
    void 미국_보통주만_내부_종목형식으로_변환한다() {
        List<FinnhubClient.FinnhubStockSymbol> response = List.of(
                symbol(
                        "USD",
                        "APPLE INC",
                        "XNAS",
                        "AAPL",
                        "Common Stock"
                ),
                symbol(
                        "USD",
                        "JPMORGAN CHASE & CO",
                        "XNYS",
                        "JPM",
                        "Common Stock"
                ),
                symbol(
                        "USD",
                        "NYSE AMERICAN COMPANY",
                        "XASE",
                        "AMEX",
                        "Common Stock"
                ),
                symbol(
                        "USD",
                        "SPDR S&P 500 ETF TRUST",
                        "ARCX",
                        "SPY",
                        "ETP"
                ),
                symbol(
                        "EUR",
                        "EUROPEAN COMPANY",
                        "XNYS",
                        "EURO",
                        "Common Stock"
                ),
                symbol(
                        "USD",
                        "OTC COMPANY",
                        "OTCM",
                        "OTC",
                        "Common Stock"
                )
        );

        List<StockSymbolData> result =
                FinnhubStockSymbolProvider.toStockSymbols(response);

        assertEquals(3, result.size());
        assertEquals("AAPL", result.getFirst().ticker());
        assertEquals("APPLE INC", result.getFirst().companyName());
        assertEquals(Exchange.NASDAQ, result.getFirst().exchange());
        assertEquals(Exchange.NYSE, result.get(1).exchange());
        assertEquals(Exchange.NYSE_AMERICAN, result.get(2).exchange());
    }

    private FinnhubClient.FinnhubStockSymbol symbol(
            String currency,
            String description,
            String mic,
            String ticker,
            String type
    ) {
        return new FinnhubClient.FinnhubStockSymbol(
                currency,
                description,
                ticker,
                mic,
                ticker,
                type
        );
    }
}
