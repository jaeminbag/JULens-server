package com.julensserver.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "lens_analyses")
public class LensAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private LensAnalysisBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    //종목 현재 주가
    @Column(name = "current_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal currentPrice;

    //종목 등락률
    @Column(name = "change_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal changeRate;

    //종목 누적 거래량
    @Column(nullable = false)
    private Long volume;

    //종목 누적 거래대금
    @Column(name = "trading_value", nullable = false, precision = 20, scale = 2)
    private BigDecimal tradingValue;

    //관련 뉴스 영향력 평가 점수
    @Column(name = "news_score", nullable = false)
    private Integer newsScore;

    //기존 움직임 평가 점수
    @Column(name = "movement_score", nullable = false)
    private Integer movementScore;

    //거래량 움직임 평가 점수
    @Column(name = "volume_score", nullable = false)
    private Integer volumeScore;

    //종목 공시/변동성/상장 유지 가능성 등의 위험도를 평가한 점수.
    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    //분석 최종 점수
    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LensLabel label;

    //분석 완료 시각
    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;


    private LensAnalysis(
            LensAnalysisBatch batch,
            Stock stock,
            BigDecimal currentPrice,
            BigDecimal changeRate,
            Long volume,
            BigDecimal tradingValue,
            Integer newsScore,
            Integer movementScore,
            Integer volumeScore,
            Integer riskScore,
            Integer totalScore,
            LensLabel label
    ) {
        this.batch = batch;
        this.stock = stock;
        this.currentPrice = currentPrice;
        this.changeRate = changeRate;
        this.volume = volume;
        this.tradingValue = tradingValue;
        this.newsScore = newsScore;
        this.movementScore = movementScore;
        this.volumeScore = volumeScore;
        this.riskScore = riskScore;
        this.totalScore = totalScore;
        this.label = label;
        this.analyzedAt = LocalDateTime.now();
    }


    public static LensAnalysis create(
            LensAnalysisBatch batch,
            Stock stock,
            BigDecimal currentPrice,
            BigDecimal changeRate,
            Long volume,
            BigDecimal tradingValue,
            Integer newsScore,
            Integer movementScore,
            Integer volumeScore,
            Integer riskScore,
            Integer totalScore,
            LensLabel label
    ) {
        return new LensAnalysis(
                batch,
                stock,
                currentPrice,
                changeRate,
                volume,
                tradingValue,
                newsScore,
                movementScore,
                volumeScore,
                riskScore,
                totalScore,
                label
        );
    }
}
