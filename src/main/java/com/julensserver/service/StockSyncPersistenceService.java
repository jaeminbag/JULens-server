package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@RequiredArgsConstructor
public class StockSyncPersistenceService {

    private final StockRepository stockRepository;
    private final KoreanCompanyNameService koreanCompanyNameService;

    @Transactional
    public StockSyncResult synchronize(
            int receivedCount,
            Map<String, StockSymbolData> symbolsByTicker
    ) {
        List<Stock> existingStocks = stockRepository.findAll();
        Map<String, Stock> stocksByTicker = existingStocks.stream()
                .collect(Collectors.toMap(
                        stock -> normalizeTicker(stock.getTicker()),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        List<Stock> newStocks = new ArrayList<>();
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
                newStocks.add(stock);
                created++;
            } else {
                // 기존 종목은 현재 트랜잭션의 관리 상태이므로 dirty checking으로 저장된다.
                stock.synchronizeMetadata(
                        symbol.companyName(),
                        koreanName,
                        symbol.exchange(),
                        symbol.currency()
                );
                updated++;
            }

            stock.activate();
            activatedTickers.add(ticker);
        }

        int deactivated = 0;
        for (Stock stock : existingStocks) {
            String ticker = normalizeTicker(stock.getTicker());
            if (stock.isActive() && !activatedTickers.contains(ticker)) {
                stock.deactivate();
                deactivated++;
            }
        }

        // 새 엔티티만 INSERT하고 기존 엔티티는 트랜잭션 종료 시 UPDATE한다.
        stockRepository.saveAll(newStocks);
        return new StockSyncResult(
                receivedCount,
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
