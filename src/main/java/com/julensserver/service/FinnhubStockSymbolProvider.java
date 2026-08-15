package com.julensserver.service;

import com.julensserver.domain.Currency;
import com.julensserver.domain.Exchange;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@Profile("real")
public class FinnhubStockSymbolProvider implements StockSymbolProvider {

    private static final String US_EXCHANGE_CODE = "US";
    private static final String COMMON_STOCK_TYPE = "Common Stock";

    private final FinnhubClient finnhubClient;

    public FinnhubStockSymbolProvider(FinnhubClient finnhubClient) {
        this.finnhubClient = finnhubClient;
    }

    @Override
    public List<StockSymbolData> getUsCommonStocks() {
        return toStockSymbols(
                finnhubClient.getStockSymbols(US_EXCHANGE_CODE)
        );
    }

    static List<StockSymbolData> toStockSymbols(
            List<FinnhubClient.FinnhubStockSymbol> symbols
    ) {
        if (symbols == null || symbols.isEmpty()) {
            return List.of();
        }

        Map<String, StockSymbolData> uniqueSymbols =
                new LinkedHashMap<>();
        for (FinnhubClient.FinnhubStockSymbol symbol : symbols) {
            toStockSymbol(symbol).ifPresent(stockSymbol ->
                    uniqueSymbols.putIfAbsent(
                            stockSymbol.ticker(),
                            stockSymbol
                    )
            );
        }
        return List.copyOf(uniqueSymbols.values());
    }

    private static Optional<StockSymbolData> toStockSymbol(
            FinnhubClient.FinnhubStockSymbol symbol
    ) {
        if (symbol == null
                || !COMMON_STOCK_TYPE.equalsIgnoreCase(symbol.type())
                || !Currency.USD.name().equalsIgnoreCase(symbol.currency())) {
            return Optional.empty();
        }

        String ticker = firstNonBlank(
                symbol.symbol(),
                symbol.displaySymbol()
        );
        String companyName = firstNonBlank(
                symbol.description(),
                symbol.displaySymbol(),
                symbol.symbol()
        );
        Optional<Exchange> exchange = toExchange(symbol.mic());
        if (ticker == null || companyName == null || exchange.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new StockSymbolData(
                ticker,
                companyName,
                exchange.get(),
                Currency.USD
        ));
    }

    private static Optional<Exchange> toExchange(String mic) {
        if (mic == null || mic.isBlank()) {
            return Optional.empty();
        }

        return switch (mic.trim().toUpperCase(Locale.ROOT)) {
            case "XNAS" -> Optional.of(Exchange.NASDAQ);
            case "XNYS" -> Optional.of(Exchange.NYSE);
            case "XASE" -> Optional.of(Exchange.NYSE_AMERICAN);
            default -> Optional.empty();
        };
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return null;
    }
}
