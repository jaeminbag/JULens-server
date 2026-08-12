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

    private static final int MAX_SUPPORTED_STOCKS = 100;
    private static final int MINIMUM_COVERAGE_PERCENT = 80;

    private final StockRepository stockRepository;
    private final StockSymbolProvider stockSymbolProvider;
    private final SupportedStockCatalog supportedStockCatalog;
    private final int maxActiveStocks;

    public StockSyncService(
            StockRepository stockRepository,
            StockSymbolProvider stockSymbolProvider,
            SupportedStockCatalog supportedStockCatalog,
            @Value("${stock.sync.max-active-stocks:100}")
            int maxActiveStocks
    ) {
        if (maxActiveStocks < 1
                || maxActiveStocks > MAX_SUPPORTED_STOCKS) {
            throw new IllegalArgumentException(
                    "활성 분석 종목 수는 1~100이어야 합니다."
            );
        }
        this.stockRepository = stockRepository;
        this.stockSymbolProvider = stockSymbolProvider;
        this.supportedStockCatalog = supportedStockCatalog;
        this.maxActiveStocks = maxActiveStocks;
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

        List<SyncTarget> syncTargets = resolveSyncTargets(symbolsByTicker);
        int minimumRequired = Math.max(
                1,
                (maxActiveStocks * MINIMUM_COVERAGE_PERCENT + 99) / 100
        );
        if (syncTargets.size() < minimumRequired) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_DATA_PROVIDER_ERROR,
                    "Finnhub 종목 목록의 지원 종목 포함률이 너무 낮습니다."
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

        for (SyncTarget target : syncTargets) {
            SupportedStockCatalog.SupportedStock supportedStock =
                    target.supportedStock();
            StockSymbolData symbol = target.symbol();
            String ticker = normalizeTicker(supportedStock.ticker());

            Stock stock = stocksByTicker.get(ticker);
            if (stock == null) {
                stock = new Stock(
                        symbol.ticker(),
                        symbol.companyName(),
                        supportedStock.companyNameKr(),
                        symbol.exchange(),
                        symbol.currency(),
                        null
                );
                stocksByTicker.put(ticker, stock);
                created++;
            } else {
                stock.synchronizeMetadata(
                        symbol.companyName(),
                        supportedStock.companyNameKr(),
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

    private List<SyncTarget> resolveSyncTargets(
            Map<String, StockSymbolData> symbolsByTicker
    ) {
        List<SyncTarget> targets = new ArrayList<>();
        for (SupportedStockCatalog.SupportedStock supportedStock
                : supportedStockCatalog.stocks()) {
            StockSymbolData symbol = symbolsByTicker.get(
                    normalizeTicker(supportedStock.ticker())
            );
            if (symbol != null) {
                targets.add(new SyncTarget(supportedStock, symbol));
            }
            if (targets.size() >= maxActiveStocks) {
                break;
            }
        }
        return targets;
    }

    private static String normalizeTicker(String ticker) {
        return ticker.trim().toUpperCase(Locale.ROOT);
    }

    private record SyncTarget(
            SupportedStockCatalog.SupportedStock supportedStock,
            StockSymbolData symbol
    ) {
    }
}
