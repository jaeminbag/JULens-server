package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.dto.lens.StockNewsData;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;


@Component
@Profile("mock")
public class MockStockNewsDataProvider
        implements StockNewsDataProvider {


    @Override
    public List<StockNewsData> getNews(Stock stock) {
        Objects.requireNonNull(
                stock,
                "뉴스를 조회할 종목은 null일 수 없습니다."
        );

        OffsetDateTime now = OffsetDateTime.now();

        return List.of(
                new StockNewsData(
                        "Company raises guidance after strong quarter",
                        "Revenue growth exceeded market expectations.",
                        "Mock Financial News",
                        "https://mock.julens.local/news/strong-quarter",
                        now.minusHours(2)
                ),
                new StockNewsData(
                        "Company announces strategic partnership",
                        "The partnership is expected to support future growth.",
                        "Mock Business News",
                        "https://mock.julens.local/news/partnership",
                        now.minusHours(5)
                )
        );
    }
}
