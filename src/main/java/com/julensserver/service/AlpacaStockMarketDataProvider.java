package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.StockMarketData;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Component
@Profile("real")
public class AlpacaStockMarketDataProvider
        implements StockMarketDataProvider {

    private static final int CANDLE_LOOKBACK_DAYS = 45;
    private static final int AVERAGE_VOLUME_DAYS = 20;

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
        AlpacaClient.AlpacaBarsResponse minuteBars =
                alpacaClient.getTodayMinuteBars(stock.getTicker());

        return toMarketData(dailyBars, minuteBars);
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

        BigDecimal previousClose = dailyBars
                .get(dailyBars.size() - 1)
                .close();
        BigDecimal currentPrice = minuteBars
                .get(minuteBars.size() - 1)
                .close();
        if (currentPrice == null || currentPrice.signum() <= 0) {
            throw providerError("Alpaca current price is invalid");
        }
        BigDecimal changeRate = previousClose == null
                || previousClose.signum() == 0
                ? BigDecimal.ZERO
                : currentPrice.subtract(previousClose)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousClose, 4, RoundingMode.HALF_UP);

        // 오늘 프리마켓부터 현재 지연 시각까지의 누적 거래량이다.
        long currentVolume = minuteBars.stream()
                .map(AlpacaClient.AlpacaBar::volume)
                .mapToLong(AlpacaStockMarketDataProvider::toNonNegativeLong)
                .sum();
        long averageVolume20d = calculateAverageVolume(dailyBars);
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
