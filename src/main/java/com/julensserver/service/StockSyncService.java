package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.StockRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Profile("real")
public class StockSyncService {

    private final StockRepository stockRepository;
    private final StockSymbolProvider stockSymbolProvider;
    private final KoreanCompanyNameService koreanCompanyNameService;
    private final int minimumSymbolCount;

    public StockSyncService(
            StockRepository stockRepository,
            StockSymbolProvider stockSymbolProvider,
            KoreanCompanyNameService koreanCompanyNameService,
            @Value("${stock.sync.minimum-symbol-count:1000}")
            int minimumSymbolCount
    ) {
        if (minimumSymbolCount < 1) {
            throw new IllegalArgumentException(
                    "최소 종목 수는 1 이상이어야 합니다."
            );
        }
        this.stockRepository = stockRepository;
        this.stockSymbolProvider = stockSymbolProvider;
        this.koreanCompanyNameService = koreanCompanyNameService;
        this.minimumSymbolCount = minimumSymbolCount;
    }

    public StockSyncResult synchronize() {
        List<StockSymbolData> receivedSymbols =
                stockSymbolProvider.getUsCommonStocks();
        if (receivedSymbols.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_DATA_PROVIDER_ERROR,
                    "Finnhub 종목 목록이 비어 있습니다."
            );
        }
        Map<String, StockSymbolData> symbolsByTicker = receivedSymbols
                .stream()
                .collect(Collectors.toMap(
                        symbol -> normalizeTicker(symbol.ticker()),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        if (symbolsByTicker.size() < minimumSymbolCount) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_DATA_PROVIDER_ERROR,
                    "Finnhub 종목 목록의 종목 수가 너무 적습니다."
            );
        }

        List<Stock> existingStocks = stockRepository.findAll();
        Map<String, Stock> stocksByTicker = existingStocks.stream()
                .collect(Collectors.toMap(
                        stock -> normalizeTicker(stock.getTicker()),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        List<Stock> stocksToSave = new ArrayList<>();
        Set<String> activatedTickers = new LinkedHashSet<>();
        int created = 0;
        int updated = 0;

        for (StockSymbolData symbol : symbolsByTicker.values()) {
            String ticker = normalizeTicker(symbol.ticker());

            Stock stock = stocksByTicker.get(ticker);
            String koreanName = koreanCompanyNameService.resolve(
                    ticker,
                    symbol.companyName(),
                    stock == null ? null : stock.getCompanyNameKr()
            );
            if (stock == null) {
                stock = new Stock(
                        ticker,
                        symbol.companyName(),
                        koreanName,
                        symbol.exchange(),
                        symbol.currency(),
                        null
                );
                stocksByTicker.put(ticker, stock);
                created++;
            } else {
                stock.synchronizeMetadata(
                        symbol.companyName(),
                        koreanName,
                        symbol.exchange(),
                        symbol.currency()
                );
                updated++;
            }

            stock.activate();
            stocksToSave.add(stock);
            activatedTickers.add(ticker);
        }

        int deactivated = 0;
        for (Stock stock : existingStocks) {
            String ticker = normalizeTicker(stock.getTicker());
            if (stock.isActive() && !activatedTickers.contains(ticker)) {
                stock.deactivate();
                stocksToSave.add(stock);
                deactivated++;
            }
        }

        stockRepository.saveAll(stocksToSave);
        return new StockSyncResult(
                receivedSymbols.size(),
                activatedTickers.size(),
                created,
                updated,
                deactivated
        );
    }

    private static String normalizeTicker(String ticker) {
        return ticker.trim().toUpperCase(Locale.ROOT);
    }
}
