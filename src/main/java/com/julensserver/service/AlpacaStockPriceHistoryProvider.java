package com.julensserver.service;

import com.julensserver.dto.stock.StockPriceHistoryResponse;
import com.julensserver.dto.stock.StockPricePointResponse;
import com.julensserver.dto.stock.StockPricePeriod;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Profile("real")
public class AlpacaStockPriceHistoryProvider
        implements StockPriceHistoryProvider {

    private static final int MAX_CHART_POINTS = 240;

    private final AlpacaClient alpacaClient;
    private final StockChartWindowResolver windowResolver;

    public AlpacaStockPriceHistoryProvider(
            AlpacaClient alpacaClient,
            StockChartWindowResolver windowResolver
    ) {
        this.alpacaClient = alpacaClient;
        this.windowResolver = windowResolver;
    }

    @Override
    public StockPriceHistoryResponse getPriceHistory(
            String ticker,
            StockPricePeriod period
    ) {
        Objects.requireNonNull(
                ticker,
                "가격 이력을 조회할 티커는 null일 수 없습니다."
        );
        Objects.requireNonNull(period, "가격 이력 기간은 null일 수 없습니다.");

        Instant now = Instant.now();
        StockChartWindowResolver.ChartWindow window =
                windowResolver.resolve(period, now);
        Instant queryEnd = period == StockPricePeriod.REALTIME
                ? earlier(
                        window.end(),
                        now.minus(
                                alpacaClient.getDataDelayMinutes(),
                                ChronoUnit.MINUTES
                        )
                )
                : window.end();

        List<AlpacaClient.AlpacaBar> bars = queryEnd.isAfter(window.start())
                ? getBars(ticker, window, queryEnd)
                : List.of();

        List<AlpacaClient.AlpacaBar> validBars = bars.stream()
                .filter(bar -> bar != null
                        && bar.timestamp() != null
                        && bar.close() != null
                        && bar.close().signum() > 0)
                .toList();
        List<StockPricePointResponse> points = downsample(validBars).stream()
                .map(bar -> new StockPricePointResponse(
                        bar.timestamp(),
                        bar.close()
                ))
                .toList();
        return new StockPriceHistoryResponse(
                ticker.trim().toUpperCase(),
                period,
                window.start(),
                window.end(),
                window.timeframe(),
                points
        );
    }

    private List<AlpacaClient.AlpacaBar> getBars(
            String ticker,
            StockChartWindowResolver.ChartWindow window,
            Instant queryEnd
    ) {
        Map<Instant, AlpacaClient.AlpacaBar> merged = new LinkedHashMap<>();
        safeHistoricalBars(
                ticker,
                window,
                queryEnd,
                "sip"
        ).forEach(bar -> merged.put(bar.timestamp(), bar));

        if (window.includeOvernight()) {
            // 무료 플랜의 과거 야간 데이터는 overnight가 아닌 BOATS 피드다.
            safeHistoricalBars(
                    ticker,
                    window,
                    queryEnd,
                    "boats"
            ).forEach(bar -> merged.put(bar.timestamp(), bar));
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(AlpacaClient.AlpacaBar::timestamp))
                .toList();
    }

    private List<AlpacaClient.AlpacaBar> safeHistoricalBars(
            String ticker,
            StockChartWindowResolver.ChartWindow window,
            Instant queryEnd,
            String feed
    ) {
        try {
            AlpacaClient.AlpacaBarsResponse response =
                    alpacaClient.getHistoricalBars(
                            ticker,
                            window.timeframe(),
                            window.start(),
                            queryEnd,
                            feed,
                            10000
                    );
            return response == null || response.bars() == null
                    ? List.of()
                    : response.bars();
        } catch (RuntimeException ignored) {
            // 한 피드가 실패해도 나머지 피드의 점으로 차트를 구성한다.
            return List.of();
        }
    }

    private Instant earlier(Instant left, Instant right) {
        return left.isBefore(right) ? left : right;
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
