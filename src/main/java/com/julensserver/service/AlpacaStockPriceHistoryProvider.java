package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.dto.stock.StockPricePointResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Profile("real")
public class AlpacaStockPriceHistoryProvider
        implements StockPriceHistoryProvider {

    private static final int DAILY_LOOKBACK_DAYS = 45;
    private static final int MAX_CHART_POINTS = 120;

    private final AlpacaClient alpacaClient;

    public AlpacaStockPriceHistoryProvider(AlpacaClient alpacaClient) {
        this.alpacaClient = alpacaClient;
    }

    @Override
    public List<StockPricePointResponse> getPriceHistory(Stock stock) {
        Objects.requireNonNull(
                stock,
                "가격 이력을 조회할 종목은 null일 수 없습니다."
        );

        // 장중에는 프리마켓부터 현재 지연 시각까지의 실제 분봉을 우선 사용한다.
        List<AlpacaClient.AlpacaBar> bars = getIntradayBars(stock);
        if (bars.isEmpty()) {
            // 주말·장 시작 전에는 최근 거래일의 일봉으로 그래프를 유지한다.
            bars = safeBars(alpacaClient.getDailyBars(
                    stock.getTicker(),
                    DAILY_LOOKBACK_DAYS
            ));
        }

        List<AlpacaClient.AlpacaBar> validBars = bars.stream()
                .filter(bar -> bar != null
                        && bar.timestamp() != null
                        && bar.close() != null
                        && bar.close().signum() > 0)
                .toList();
        return downsample(validBars).stream()
                .map(bar -> new StockPricePointResponse(
                        bar.timestamp(),
                        bar.close()
                ))
                .toList();
    }

    private List<AlpacaClient.AlpacaBar> getIntradayBars(Stock stock) {
        try {
            return safeBars(alpacaClient.getTodayMinuteBars(
                    stock.getTicker()
            ));
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private List<AlpacaClient.AlpacaBar> safeBars(
            AlpacaClient.AlpacaBarsResponse response
    ) {
        return response == null || response.bars() == null
                ? List.of()
                : response.bars();
    }

    private List<AlpacaClient.AlpacaBar> downsample(
            List<AlpacaClient.AlpacaBar> bars
    ) {
        if (bars.size() <= MAX_CHART_POINTS) {
            return bars;
        }

        List<AlpacaClient.AlpacaBar> sampled = new ArrayList<>();
        double step = (double) (bars.size() - 1)
                / (MAX_CHART_POINTS - 1);
        for (int index = 0; index < MAX_CHART_POINTS; index++) {
            sampled.add(bars.get((int) Math.round(index * step)));
        }
        return List.copyOf(sampled);
    }
}
