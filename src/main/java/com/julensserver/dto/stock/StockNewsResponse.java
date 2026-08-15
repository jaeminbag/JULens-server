package com.julensserver.dto.stock;

import com.julensserver.domain.StockNews;

import java.time.OffsetDateTime;
import java.util.List;

public record StockNewsResponse(
        Long newsId,
        String title,
        String summary,
        String source,
        String url,
        OffsetDateTime publishedAt,
        List<RelatedStockResponse> relatedStocks
) {
    public static StockNewsResponse from(StockNews news) {
        return new StockNewsResponse(
                news.getId(),
                news.getTitle(),
                news.getSummary(),
                news.getSource(),
                news.getUrl(),
                news.getPublishedAt(),
                news.getRelatedStocks().stream()
                        .map(RelatedStockResponse::from)
                        .sorted((left, right) ->
                                left.ticker().compareToIgnoreCase(right.ticker())
                        )
                        .toList()
        );
    }
}
