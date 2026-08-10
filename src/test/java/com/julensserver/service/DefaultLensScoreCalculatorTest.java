package com.julensserver.service;

import com.julensserver.domain.LensLabel;
import com.julensserver.dto.lens.LensAnalysisResult;
import com.julensserver.dto.lens.StockMarketData;
import com.julensserver.dto.lens.StockNewsData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultLensScoreCalculatorTest {

    private final DefaultLensScoreCalculator calculator =
            new DefaultLensScoreCalculator();

    @Test
    void 긍정적인_뉴스와_상승세와_거래량_증가가_있으면_매수후보로_분류한다() {
        StockMarketData marketData = new StockMarketData(
                new BigDecimal("25.50"),
                new BigDecimal("7.50"),
                3_000_000L,
                1_000_000L,
                new BigDecimal("76500000.00")
        );

        List<StockNewsData> newsList = List.of(
                new StockNewsData(
                        "Company raises guidance after strong quarter",
                        "Revenue growth exceeded market expectations.",
                        "Mock Financial News",
                        OffsetDateTime.now().minusHours(2)
                ),
                new StockNewsData(
                        "Company announces strategic partnership",
                        "The partnership is expected to support future growth.",
                        "Mock Business News",
                        OffsetDateTime.now().minusHours(5)
                )
        );

        LensAnalysisResult result =
                calculator.calculate(marketData, newsList);

        assertEquals(20, result.newsScore());
        assertEquals(30, result.movementScore());
        assertEquals(30, result.volumeScore());
        assertEquals(10, result.riskScore());
        assertEquals(80, result.totalScore());
        assertEquals(
                LensLabel.CONDITION_BUY_CANDIDATE,
                result.label()
        );
    }

    @Test
    void 치명적인_위험_뉴스가_있으면_위험으로_분류한다() {
        StockMarketData marketData = new StockMarketData(
                new BigDecimal("25.50"),
                new BigDecimal("7.50"),
                3_000_000L,
                1_000_000L,
                new BigDecimal("76500000.00")
        );

        List<StockNewsData> newsList = List.of(
                new StockNewsData(
                        "Company faces possible delisting",
                        "The exchange issued a delisting warning.",
                        "Mock Financial News",
                        OffsetDateTime.now()
                )
        );

        LensAnalysisResult result =
                calculator.calculate(marketData, newsList);

        assertEquals(80, result.riskScore());
        assertEquals(LensLabel.RISK, result.label());
    }

    @Test
    void 주가가_이미_15퍼센트_이상_상승했다면_늦은진입으로_분류한다() {
        StockMarketData marketData = new StockMarketData(
                new BigDecimal("30.00"),
                new BigDecimal("16.00"),
                2_000_000L,
                1_000_000L,
                new BigDecimal("60000000.00")
        );

        LensAnalysisResult result =
                calculator.calculate(marketData, List.of());

        assertEquals(0, result.movementScore());
        assertEquals(30, result.volumeScore());
        assertEquals(30, result.riskScore());
        assertEquals(30, result.totalScore());
        assertEquals(LensLabel.ALREADY_LATE, result.label());
    }

    @Test
    void 점수가_매수후보_기준보다_낮으면_관망으로_분류한다() {
        StockMarketData marketData = new StockMarketData(
                new BigDecimal("20.00"),
                new BigDecimal("1.00"),
                800_000L,
                1_000_000L,
                new BigDecimal("16000000.00")
        );

        LensAnalysisResult result =
                calculator.calculate(marketData, List.of());

        assertEquals(0, result.newsScore());
        assertEquals(10, result.movementScore());
        assertEquals(0, result.volumeScore());
        assertEquals(0, result.riskScore());
        assertEquals(10, result.totalScore());
        assertEquals(LensLabel.WATCH, result.label());
    }
}