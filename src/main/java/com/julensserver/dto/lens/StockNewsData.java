package com.julensserver.dto.lens;

import java.time.OffsetDateTime;

public record StockNewsData(
        String title,
        String summary,
        String source,
        String url,
        OffsetDateTime publishedAt
) {
}
