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
        return toMarketData(alpacaClient.getDailyBars(
                stock.getTicker(),
                CANDLE_LOOKBACK_DAYS
        ));
    }

    static StockMarketData toMarketData(
            AlpacaClient.AlpacaBarsResponse response
    ) {
        List<AlpacaClient.AlpacaBar> bars = response.bars();
        if (bars == null || bars.isEmpty()) {
            throw providerError("Alpaca returned no bar data");
        }

        int lastIndex = bars.size() - 1;
        BigDecimal currentPrice = bars.get(lastIndex).close();
        if (currentPrice == null || currentPrice.signum() <= 0) {
            throw providerError("Alpaca current price is invalid");
        }

        BigDecimal previousClose = lastIndex > 0
                ? bars.get(lastIndex - 1).close()
                : currentPrice;
        BigDecimal changeRate = previousClose == null
                || previousClose.signum() == 0
                ? BigDecimal.ZERO
                : currentPrice.subtract(previousClose)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousClose, 4, RoundingMode.HALF_UP);

        long currentVolume = toNonNegativeLong(
                bars.get(lastIndex).volume()
        );
        long averageVolume20d = calculateAverageVolume(
                bars,
                lastIndex,
                currentVolume
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

    private static long calculateAverageVolume(
            List<AlpacaClient.AlpacaBar> bars,
            int currentIndex,
            long fallback
    ) {
        int start = Math.max(0, currentIndex - AVERAGE_VOLUME_DAYS);
        if (start == currentIndex) {
            return fallback;
        }

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (int index = start; index < currentIndex; index++) {
            BigDecimal volume = bars.get(index).volume();
            if (volume != null && volume.signum() >= 0) {
                sum = sum.add(volume);
                count++;
            }
        }
        if (count == 0) {
            return fallback;
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
