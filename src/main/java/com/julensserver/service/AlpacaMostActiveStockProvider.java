package com.julensserver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Component
@Profile("real")
public class AlpacaMostActiveStockProvider
        implements MostActiveStockProvider {

    private final AlpacaClient alpacaClient;
    private final int candidateCount;

    public AlpacaMostActiveStockProvider(
            AlpacaClient alpacaClient,
            @Value("${lens.analysis.candidate-count:100}")
            int candidateCount
    ) {
        if (candidateCount < 1 || candidateCount > 100) {
            throw new IllegalArgumentException(
                    "거래량 상위 분석 후보 수는 1~100이어야 합니다."
            );
        }
        this.alpacaClient = alpacaClient;
        this.candidateCount = candidateCount;
    }

    @Override
    public List<String> getMostActiveTickers() {
        List<AlpacaClient.AlpacaActiveStock> mostActives =
                alpacaClient.getMostActiveStocks(candidateCount)
                        .mostActives();
        if (mostActives == null || mostActives.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> tickers = new LinkedHashSet<>();
        for (AlpacaClient.AlpacaActiveStock stock : mostActives) {
            if (stock != null
                    && stock.symbol() != null
                    && !stock.symbol().isBlank()) {
                tickers.add(stock.symbol().trim()
                        .toUpperCase(Locale.ROOT));
            }
        }
        return List.copyOf(tickers);
    }
}
