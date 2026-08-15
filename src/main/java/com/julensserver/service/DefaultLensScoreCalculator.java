package com.julensserver.service;

import com.julensserver.domain.LensLabel;
import com.julensserver.dto.lens.LensAnalysisResult;
import com.julensserver.dto.lens.StockMarketData;
import com.julensserver.dto.lens.StockNewsData;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class DefaultLensScoreCalculator implements LensScoreCalculator {

    private static final List<String> POSITIVE_NEWS_KEYWORDS = List.of(
            "raises guidance",
            "beats estimates",
            "beat estimates",
            "revenue growth",
            "profit growth",
            "analyst upgrade",
            "approval",
            "partnership",
            "strategic agreement",
            "wins contract",
            "new contract"
    );

    private static final List<String> NEGATIVE_NEWS_KEYWORDS = List.of(
            "lowers guidance",
            "misses estimates",
            "missed estimates",
            "analyst downgrade",
            "investigation",
            "lawsuit",
            "secondary offering",
            "public offering",
            "dilution",
            "recall"
    );

    private static final List<String> CRITICAL_RISK_KEYWORDS = List.of(
            "bankruptcy",
            "delisting",
            "going concern",
            "accounting fraud"
    );

    @Override
    public LensAnalysisResult calculate(
            StockMarketData marketData,
            List<StockNewsData> newsList
    ) {
        validateMarketData(marketData);

        List<StockNewsData> safeNewsList =
                newsList == null ? List.of() : newsList;

        BigDecimal volumeRatio = calculateVolumeRatio(marketData);

        int newsScore = calculateNewsScore(safeNewsList);
        int movementScore = calculateMovementScore(
                marketData.changeRate()
        );
        int volumeScore = calculateVolumeScore(volumeRatio);

        int totalScore =
                newsScore
                        + movementScore
                        + volumeScore;

        int riskScore = calculateRiskScore(
                marketData.changeRate(),
                volumeRatio,
                safeNewsList
        );

        LensLabel label = determineLabel(
                totalScore,
                riskScore,
                marketData.changeRate()
        );

        return new LensAnalysisResult(
                marketData.currentPrice(),
                marketData.changeRate(),
                marketData.volume(),
                marketData.tradingValue(),
                newsScore,
                movementScore,
                volumeScore,
                riskScore,
                totalScore,
                label
        );
    }

    private int calculateNewsScore(
            List<StockNewsData> newsList
    ) {
        int positiveNewsCount = 0;

        for (StockNewsData news : newsList) {
            if (containsAnyKeyword(
                    news,
                    POSITIVE_NEWS_KEYWORDS
            )) {
                positiveNewsCount++;
            }
        }

        return Math.min(positiveNewsCount * 10, 40);
    }


    private int calculateMovementScore(
            BigDecimal changeRate
    ) {
        if (changeRate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        if (changeRate.compareTo(
                BigDecimal.valueOf(2)
        ) < 0) {
            return 10;
        }

        if (changeRate.compareTo(
                BigDecimal.valueOf(5)
        ) < 0) {
            return 20;
        }

        if (changeRate.compareTo(
                BigDecimal.valueOf(10)
        ) < 0) {
            return 30;
        }

        if (changeRate.compareTo(
                BigDecimal.valueOf(15)
        ) < 0) {
            return 20;
        }

        return 0;
    }

    private int calculateVolumeScore(
            BigDecimal volumeRatio
    ) {
        if (volumeRatio.compareTo(
                BigDecimal.ONE
        ) < 0) {
            return 0;
        }

        if (volumeRatio.compareTo(
                BigDecimal.valueOf(1.5)
        ) < 0) {
            return 10;
        }

        if (volumeRatio.compareTo(
                BigDecimal.valueOf(2)
        ) < 0) {
            return 20;
        }

        return 30;
    }

    private int calculateRiskScore(
            BigDecimal changeRate,
            BigDecimal volumeRatio,
            List<StockNewsData> newsList
    ) {
        int riskScore = calculateNewsRiskScore(newsList);

        if (changeRate.compareTo(
                BigDecimal.valueOf(-15)
        ) <= 0) {
            riskScore += 50;
        } else if (changeRate.compareTo(
                BigDecimal.valueOf(-10)
        ) <= 0) {
            riskScore += 40;
        } else if (changeRate.compareTo(
                BigDecimal.valueOf(-5)
        ) <= 0) {
            riskScore += 20;
        } else if (changeRate.compareTo(
                BigDecimal.valueOf(20)
        ) >= 0) {
            riskScore += 40;
        } else if (changeRate.compareTo(
                BigDecimal.valueOf(15)
        ) >= 0) {
            riskScore += 30;
        } else if (changeRate.compareTo(
                BigDecimal.valueOf(10)
        ) >= 0) {
            riskScore += 15;
        }

        if (volumeRatio.compareTo(
                BigDecimal.valueOf(10)
        ) >= 0) {
            riskScore += 30;
        } else if (volumeRatio.compareTo(
                BigDecimal.valueOf(5)
        ) >= 0) {
            riskScore += 20;
        } else if (volumeRatio.compareTo(
                BigDecimal.valueOf(3)
        ) >= 0) {
            riskScore += 10;
        }

        return Math.min(riskScore, 100);
    }

    private int calculateNewsRiskScore(
            List<StockNewsData> newsList
    ) {
        int negativeNewsCount = 0;

        for (StockNewsData news : newsList) {
            if (containsAnyKeyword(
                    news,
                    CRITICAL_RISK_KEYWORDS
            )) {
                return 70;
            }

            if (containsAnyKeyword(
                    news,
                    NEGATIVE_NEWS_KEYWORDS
            )) {
                negativeNewsCount++;
            }
        }

        return Math.min(negativeNewsCount * 15, 45);
    }


    private LensLabel determineLabel(
            int totalScore,
            int riskScore,
            BigDecimal changeRate
    ) {
        if (riskScore >= 70) {
            return LensLabel.RISK;
        }

        if (changeRate.compareTo(
                BigDecimal.valueOf(15)
        ) >= 0) {
            return LensLabel.ALREADY_LATE;
        }

        if (totalScore >= 65) {
            return LensLabel.CONDITION_BUY_CANDIDATE;
        }

        return LensLabel.WATCH;
    }

    private BigDecimal calculateVolumeRatio(
            StockMarketData marketData
    ) {
        Long volume = marketData.volume();
        Long averageVolume20d =
                marketData.averageVolume20d();

        if (volume == null
                || averageVolume20d == null
                || averageVolume20d <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(volume)
                .divide(
                        BigDecimal.valueOf(averageVolume20d),
                        4,
                        RoundingMode.HALF_UP
                );
    }

    private boolean containsAnyKeyword(
            StockNewsData news,
            List<String> keywords
    ) {
        if (news == null) {
            return false;
        }

        String text = (
                normalize(news.title())
                        + " "
                        + normalize(news.summary())
        ).toLowerCase(Locale.ROOT);

        return keywords.stream()
                .anyMatch(text::contains);
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }

    private void validateMarketData(
            StockMarketData marketData
    ) {
        Objects.requireNonNull(
                marketData,
                "시장 데이터는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                marketData.currentPrice(),
                "현재가는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                marketData.changeRate(),
                "등락률은 null일 수 없습니다."
        );
        Objects.requireNonNull(
                marketData.volume(),
                "거래량은 null일 수 없습니다."
        );
        Objects.requireNonNull(
                marketData.tradingValue(),
                "거래대금은 null일 수 없습니다."
        );
    }
}