package com.julensserver;

import com.julensserver.domain.Currency;
import com.julensserver.domain.Exchange;
import com.julensserver.domain.LensAnalysis;
import com.julensserver.domain.LensAnalysisBatch;
import com.julensserver.domain.LensLabel;
import com.julensserver.domain.MarketSession;
import com.julensserver.domain.Stock;
import com.julensserver.domain.User;
import com.julensserver.domain.UserStock;
import com.julensserver.dto.lens.StockNewsData;
import com.julensserver.dto.stock.StockNewsResponse;
import com.julensserver.dto.stock.UserStockLatestResponse;
import com.julensserver.repository.LensAnalysisBatchRepository;
import com.julensserver.repository.LensAnalysisRepository;
import com.julensserver.repository.StockNewsRepository;
import com.julensserver.repository.StockRepository;
import com.julensserver.repository.UserRepository;
import com.julensserver.repository.UserStockRepository;
import com.julensserver.service.StockNewsService;
import com.julensserver.service.UserStockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles({"test", "mock"})
@Transactional
class LensFeatureIntegrationTests {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockNewsRepository stockNewsRepository;

    @Autowired
    private StockNewsService stockNewsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserStockRepository userStockRepository;

    @Autowired
    private LensAnalysisBatchRepository batchRepository;

    @Autowired
    private LensAnalysisRepository analysisRepository;

    @Autowired
    private UserStockService userStockService;

    @Test
    void 같은_URL_뉴스는_한번만_저장하고_여러_종목을_연결한다() {
        Stock micron = saveStock("MU", "Micron", "마이크론");
        Stock nvidia = saveStock("NVDA", "NVIDIA", "엔비디아");
        StockNewsData article = new StockNewsData(
                "Semiconductor demand improves",
                "Memory and AI chip demand improved.",
                "Reuters",
                "https://example.com/semiconductor-demand",
                OffsetDateTime.now()
        );

        stockNewsService.saveForStock(micron, List.of(article));
        stockNewsService.saveForStock(nvidia, List.of(article));

        assertEquals(1L, stockNewsRepository.count());

        Page<StockNewsResponse> result = stockNewsService.search(
                "마이크론",
                null,
                0,
                20
        );
        assertEquals(1, result.getTotalElements());
        assertEquals(2, result.getContent().getFirst().relatedStocks().size());
    }

    @Test
    void 관심종목에_최신_완료배치의_분석결과를_결합한다() {
        Stock micron = saveStock("MU", "Micron", "마이크론");
        User user = userRepository.save(new User(
                "lens-test@example.com",
                "encoded-password",
                "lensTester"
        ));
        userStockRepository.save(new UserStock(user, micron));

        LensAnalysisBatch batch = batchRepository.save(
                LensAnalysisBatch.start(MarketSession.REGULAR_MARKET)
        );
        analysisRepository.save(LensAnalysis.create(
                batch,
                micron,
                new BigDecimal("120.00"),
                new BigDecimal("3.50"),
                2_000_000L,
                new BigDecimal("240000000.00"),
                20,
                20,
                20,
                0,
                60,
                LensLabel.WATCH
        ));
        batch.complete();

        List<UserStockLatestResponse> result =
                userStockService.getUserStocksLatest(user.getId());

        assertEquals(1, result.size());
        assertNotNull(result.getFirst().latestAnalysis());
        assertEquals(
                new BigDecimal("120.00"),
                result.getFirst().latestAnalysis().getCurrentPrice()
        );
    }

    private Stock saveStock(
            String ticker,
            String companyName,
            String companyNameKr
    ) {
        Stock stock = new Stock(
                ticker,
                companyName,
                companyNameKr,
                Exchange.NASDAQ,
                Currency.USD,
                "Technology"
        );
        stock.activate();
        return stockRepository.save(stock);
    }
}
