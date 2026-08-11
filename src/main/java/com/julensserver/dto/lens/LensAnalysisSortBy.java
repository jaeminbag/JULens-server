package com.julensserver.dto.lens;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LensAnalysisSortBy {
    TOTAL_SCORE("totalScore"),
    COMPANY_NAME("stock.companyNameKr"),
    CURRENT_PRICE("currentPrice"),
    CHANGE_RATE("changeRate"),
    VOLUME("volume"),
    ANALYZED_AT("analyzedAt");

    private final String property;

    public org.springframework.data.domain.Sort toSort(
            org.springframework.data.domain.Sort.Direction direction
    ) {
        org.springframework.data.domain.Sort primary =
                org.springframework.data.domain.Sort.by(direction, property);

        if (this == COMPANY_NAME) {
            return primary
                    .and(org.springframework.data.domain.Sort.by(
                            direction,
                            "stock.companyName"
                    ))
                    .and(org.springframework.data.domain.Sort.by(
                            direction,
                            "stock.ticker"
                    ));
        }

        return primary.and(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC,
                "id"
        ));
    }
}
