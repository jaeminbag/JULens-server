package com.julensserver.controller;

import com.julensserver.domain.MarketSession;
import com.julensserver.dto.common.ApiResponse;
import com.julensserver.service.LensAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("mock")
@RestController
@RequiredArgsConstructor
@RequestMapping("/lens-analyses")
public class MockLensAnalysisController {

    private final LensAnalysisService lensAnalysisService;

    @PostMapping("/run")
    public ApiResponse<Void> runAnalysis(
            @RequestParam MarketSession marketSession
    ) {
        lensAnalysisService.runAnalysis(marketSession);
        return ApiResponse.success("Lens 분석이 완료되었습니다.");
    }
}
