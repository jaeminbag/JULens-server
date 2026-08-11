package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.StockNewsData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
@Profile("real")
public class FinnhubStockNewsDataProvider
        implements StockNewsDataProvider {

    private static final int MAX_NEWS_PER_STOCK = 50;

    private final FinnhubClient finnhubClient;
    private final int lookbackDays;

    public FinnhubStockNewsDataProvider(
            FinnhubClient finnhubClient,
            @Value("${finnhub.news-lookback-days:7}") int lookbackDays
    ) {
        this.finnhubClient = finnhubClient;
        this.lookbackDays = lookbackDays;
    }

    @Override
    public List<StockNewsData> getNews(Stock stock) {
        Objects.requireNonNull(
                stock,
                "뉴스를 조회할 종목은 null일 수 없습니다."
        );

        return finnhubClient.getCompanyNews(
                        stock.getTicker(),
                        lookbackDays
                ).stream()
                .filter(item -> item.headline() != null
                        && !item.headline().isBlank()
                        && item.url() != null
                        && !item.url().isBlank())
                .sorted(Comparator.comparing(
                        FinnhubClient.FinnhubNewsItem::datetime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(MAX_NEWS_PER_STOCK)
                .map(FinnhubStockNewsDataProvider::toNewsData)
                .toList();
    }

    static StockNewsData toNewsData(
            FinnhubClient.FinnhubNewsItem item
    ) {
        return new StockNewsData(
                item.headline(),
                item.summary(),
                item.source(),
                item.url(),
                item.datetime() == null
                        ? null
                        : Instant.ofEpochSecond(item.datetime())
                        .atOffset(ZoneOffset.UTC)
        );
    }
}
