package com.julensserver.controller;

import com.julensserver.domain.MarketSession;
import com.julensserver.dto.common.ApiResponse;
import com.julensserver.dto.lens.LensAnalysisResponse;
import com.julensserver.dto.lens.LensAnalysisSortBy;
import com.julensserver.service.LensAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Profile("mock")
@RestController
@RequiredArgsConstructor
@RequestMapping("/lens-analyses")
public class LensAnalysisController {

    private final LensAnalysisService lensAnalysisService;

    @GetMapping("/latest")
    public ApiResponse<List<LensAnalysisResponse>> getLatestAnalyses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "TOTAL_SCORE") LensAnalysisSortBy sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {

        List<LensAnalysisResponse> lensAnalysisResponses =
                lensAnalysisService.getLatestAnalyses(
                        keyword,
                        minPrice,
                        maxPrice,
                        sortBy,
                        direction
                );

        return ApiResponse.success(
                "최신 종목 분석 결과 조회에 성공했습니다.",
                lensAnalysisResponses
        );
    }

    @PostMapping("/run")
    public ApiResponse<Void> runAnalysis(
            @RequestParam MarketSession marketSession
    ) {
        lensAnalysisService.runAnalysis(marketSession);

        return ApiResponse.success(
                "Lens 분석이 완료되었습니다."
        );
    }
}
