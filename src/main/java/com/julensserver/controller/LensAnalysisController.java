package com.julensserver.controller;

import com.julensserver.dto.common.ApiResponse;
import com.julensserver.dto.lens.LensAnalysisResponse;
import com.julensserver.service.LensAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/lens-analyses")
public class LensAnalysisController {

    private final LensAnalysisService lensAnalysisService;

    @GetMapping("/latest")
    public ApiResponse<List<LensAnalysisResponse>> getLatestAnalyses() {

        List<LensAnalysisResponse> lensAnalysisResponses =
                lensAnalysisService.getLatestAnalyses();

        return ApiResponse.success(
                "최신 종목 분석 결과 조회에 성공했습니다.",
                lensAnalysisResponses
        );
    }
}