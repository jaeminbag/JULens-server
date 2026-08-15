package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.StockMarketData;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Component
@Profile("real")
public class AlpacaStockMarketDataProvider
        implements StockMarketDataProvider {

    private static final int CANDLE_LOOKBACK_DAYS = 45;
    private static final int AVERAGE_VOLUME_DAYS = 20;
    private static final ZoneId NEW_YORK =
            ZoneId.of("America/New_York");

    private final AlpacaClient alpacaClient;

    public AlpacaStockMarketDataProvider(AlpacaClient alpacaClient) {
        this.alpacaClient = alpacaClient;
    }

    @Override
    public StockMarketData getMarketData(Stock stock) {
        Objects.requireNonNull(
                stock,
                "시세를 조회할 종목은 null일 수 없습니다."
        );
        AlpacaClient.AlpacaBarsResponse dailyBars =
                alpacaClient.getDailyBars(
                        stock.getTicker(),
                        CANDLE_LOOKBACK_DAYS
                );
        AlpacaClient.AlpacaBarsResponse minuteBars = resolveMinuteBars(
                stock.getTicker(),
                dailyBars
        );

        return toMarketData(dailyBars, minuteBars);
    }

    private AlpacaClient.AlpacaBarsResponse resolveMinuteBars(
            String ticker,
            AlpacaClient.AlpacaBarsResponse dailyResponse
    ) {
        List<AlpacaClient.AlpacaBar> dailyBars = dailyResponse.bars();
        if (dailyBars == null || dailyBars.isEmpty()) {
            throw providerError("Alpaca returned no daily bar data");
        }

        if (alpacaClient.isTodayDelayedDataAvailable()) {
            AlpacaClient.AlpacaBarsResponse todayResponse =
                    alpacaClient.getTodayMinuteBars(ticker);
            if (hasBars(todayResponse)) {
                return todayResponse;
            }
        }

        // 오늘 분봉이 없는 주말·휴장일에는 일봉이 알려주는 최근 거래일을 사용한다.
        AlpacaClient.AlpacaBar latestDailyBar =
                dailyBars.get(dailyBars.size() - 1);
        if (latestDailyBar.timestamp() == null) {
            throw providerError("Alpaca daily bar timestamp is missing");
        }
        LocalDate latestTradingDate = latestDailyBar.timestamp()
                .atZone(NEW_YORK)
                .toLocalDate();
        return alpacaClient.getMinuteBarsForTradingDate(
                ticker,
                latestTradingDate
        );
    }

    private static boolean hasBars(
            AlpacaClient.AlpacaBarsResponse response
    ) {
        return response != null
                && response.bars() != null
                && !response.bars().isEmpty();
    }

    static StockMarketData toMarketData(
            AlpacaClient.AlpacaBarsResponse dailyResponse,
            AlpacaClient.AlpacaBarsResponse minuteResponse
    ) {
        List<AlpacaClient.AlpacaBar> dailyBars = dailyResponse.bars();
        if (dailyBars == null || dailyBars.isEmpty()) {
            throw providerError("Alpaca returned no daily bar data");
        }
        List<AlpacaClient.AlpacaBar> minuteBars = minuteResponse.bars();
        if (minuteBars == null || minuteBars.isEmpty()) {
            throw providerError("Alpaca returned no intraday bar data");
        }

        AlpacaClient.AlpacaBar currentBar =
                minuteBars.get(minuteBars.size() - 1);
        int previousCloseIndex = resolvePreviousCloseIndex(
                dailyBars,
                currentBar
        );
        BigDecimal previousClose = dailyBars
                .get(previousCloseIndex)
                .close();
        BigDecimal currentPrice = currentBar.close();
        if (currentPrice == null || currentPrice.signum() <= 0) {
            throw providerError("Alpaca current price is invalid");
        }
        BigDecimal changeRate = previousClose == null
                || previousClose.signum() == 0
                ? BigDecimal.ZERO
                : currentPrice.subtract(previousClose)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousClose, 4, RoundingMode.HALF_UP);

        // 선택한 거래일의 프리마켓부터 마지막 분봉까지 누적한 거래량이다.
        long currentVolume = minuteBars.stream()
                .map(AlpacaClient.AlpacaBar::volume)
                .mapToLong(AlpacaStockMarketDataProvider::toNonNegativeLong)
                .sum();
        // fallback 거래일 자체는 평균 거래량 기준에서 제외한다.
        long averageVolume20d = calculateAverageVolume(
                dailyBars.subList(0, previousCloseIndex + 1)
        );
        BigDecimal tradingValue = currentPrice
                .multiply(BigDecimal.valueOf(currentVolume))
                .setScale(2, RoundingMode.HALF_UP);

        return new StockMarketData(
                currentPrice,
                changeRate,
                currentVolume,
                averageVolume20d,
                tradingValue
        );
    }

    private static int resolvePreviousCloseIndex(
            List<AlpacaClient.AlpacaBar> dailyBars,
            AlpacaClient.AlpacaBar currentBar
    ) {
        int index = dailyBars.size() - 1;
        AlpacaClient.AlpacaBar latestDailyBar = dailyBars.get(index);

        if (currentBar.timestamp() != null
                && latestDailyBar.timestamp() != null) {
            LocalDate currentTradingDate = currentBar.timestamp()
                    .atZone(NEW_YORK)
                    .toLocalDate();
            LocalDate latestDailyDate = latestDailyBar.timestamp()
                    .atZone(NEW_YORK)
                    .toLocalDate();
            if (currentTradingDate.equals(latestDailyDate)) {
                index--;
            }
        }

        if (index < 0) {
            throw providerError(
                    "Alpaca returned no previous close for intraday data"
            );
        }
        return index;
    }

    private static long calculateAverageVolume(
            List<AlpacaClient.AlpacaBar> bars
    ) {
        int start = Math.max(0, bars.size() - AVERAGE_VOLUME_DAYS);

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (int index = start; index < bars.size(); index++) {
            BigDecimal volume = bars.get(index).volume();
            if (volume != null && volume.signum() >= 0) {
                sum = sum.add(volume);
                count++;
            }
        }
        if (count == 0) {
            return 0L;
        }
        return sum.divide(
                BigDecimal.valueOf(count),
                0,
                RoundingMode.HALF_UP
        ).longValue();
    }

    private static long toNonNegativeLong(BigDecimal value) {
        return value == null ? 0L : Math.max(0L, value.longValue());
    }

    private static BusinessException providerError(String detail) {
        return new BusinessException(
                ErrorCode.EXTERNAL_DATA_PROVIDER_ERROR,
                detail
        );
    }
}
