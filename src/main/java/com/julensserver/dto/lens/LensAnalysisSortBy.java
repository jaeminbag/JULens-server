package com.julensserver.dto.lens;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LensAnalysisSortBy {
    TOTAL_SCORE("totalScore"),
    CURRENT_PRICE("currentPrice"),
    CHANGE_RATE("changeRate"),
    VOLUME("volume"),
    ANALYZED_AT("analyzedAt");

    private final String property;
}
