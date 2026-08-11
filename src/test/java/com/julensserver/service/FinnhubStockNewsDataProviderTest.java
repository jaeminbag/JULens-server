package com.julensserver.service;

import com.julensserver.dto.lens.StockNewsData;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinnhubStockNewsDataProviderTest {

    @Test
    void 핀허브_뉴스를_내부_뉴스형식으로_변환한다() {
        FinnhubClient.FinnhubNewsItem item =
                new FinnhubClient.FinnhubNewsItem(
                        "Micron raises guidance",
                        "Demand improved.",
                        "Reuters",
                        "https://example.com/mu-guidance",
                        1_788_825_600L
                );

        StockNewsData result =
                FinnhubStockNewsDataProvider.toNewsData(item);

        assertEquals("Micron raises guidance", result.title());
        assertEquals("https://example.com/mu-guidance", result.url());
        assertEquals(
                Instant.ofEpochSecond(1_788_825_600L)
                        .atOffset(ZoneOffset.UTC),
                result.publishedAt()
        );
    }
}
