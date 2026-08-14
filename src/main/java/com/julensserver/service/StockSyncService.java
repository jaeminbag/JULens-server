package com.julensserver.service;

import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Profile("real")
public class StockSyncService {

    private final StockSymbolProvider stockSymbolProvider;
    private final StockSyncPersistenceService persistenceService;
    private final int minimumSymbolCount;

    public StockSyncService(
            StockSymbolProvider stockSymbolProvider,
            StockSyncPersistenceService persistenceService,
            @Value("${stock.sync.minimum-symbol-count:1000}")
            int minimumSymbolCount
    ) {
        if (minimumSymbolCount < 1) {
            throw new IllegalArgumentException(
                    "최소 종목 수는 1 이상이어야 합니다."
            );
        }
        this.stockSymbolProvider = stockSymbolProvider;
        this.persistenceService = persistenceService;
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

        // Finnhub HTTP 호출이 끝난 뒤에만 짧은 DB 저장 트랜잭션을 연다.
        return persistenceService.synchronize(
                receivedSymbols.size(),
                symbolsByTicker
        );
    }

    private static String normalizeTicker(String ticker) {
        return ticker.trim().toUpperCase(Locale.ROOT);
    }
}
