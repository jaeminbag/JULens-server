package com.julensserver.dto.lens;

import com.julensserver.domain.Currency;
import com.julensserver.domain.Exchange;
import com.julensserver.domain.LensAnalysis;
import com.julensserver.domain.LensLabel;
import com.julensserver.domain.MarketSession;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 종목 분석 결과를 클라이언트에 전달하는 응답 DTO다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LensAnalysisResponse {

    // 분석 결과 ID
    private Long analysisId;

    // 분석 대상 종목의 기본 정보
    private Long stockId;
    private String ticker;
    private String companyName;
    private String companyNameKr;
    private Exchange exchange;
    private Currency currency;
    private String sector;

    // 분석 당시 시장 구간
    private MarketSession marketSession;

    // 분석 당시 시장 데이터
    private BigDecimal currentPrice;
    private BigDecimal changeRate;
    private Long volume;
    private BigDecimal tradingValue;

    // 항목별 분석 점수
    private Integer newsScore;
    private Integer movementScore;
    private Integer volumeScore;
    private Integer riskScore;

    // 최종 분석 결과
    private Integer totalScore;
    private LensLabel label;

    // 분석 완료 시각
    private LocalDateTime analyzedAt;

    /**
     * LensAnalysis 엔티티를 응답 DTO로 변환한다.
     */
    public static LensAnalysisResponse from(LensAnalysis lensAnalysis) {
        return new LensAnalysisResponse(
                lensAnalysis.getId(),

                lensAnalysis.getStock().getId(),
                lensAnalysis.getStock().getTicker(),
                lensAnalysis.getStock().getCompanyName(),
                lensAnalysis.getStock().getCompanyNameKr(),
                lensAnalysis.getStock().getExchange(),
                lensAnalysis.getStock().getCurrency(),
                lensAnalysis.getStock().getSector(),

                lensAnalysis.getBatch().getMarketSession(),

                lensAnalysis.getCurrentPrice(),
                lensAnalysis.getChangeRate(),
                lensAnalysis.getVolume(),
                lensAnalysis.getTradingValue(),

                lensAnalysis.getNewsScore(),
                lensAnalysis.getMovementScore(),
                lensAnalysis.getVolumeScore(),
                lensAnalysis.getRiskScore(),

                lensAnalysis.getTotalScore(),
                lensAnalysis.getLabel(),

                lensAnalysis.getAnalyzedAt()
        );
    }
}