package com.julensserver.service;

import com.julensserver.domain.Stock;
import com.julensserver.domain.StockNews;
import com.julensserver.dto.lens.StockNewsData;
import com.julensserver.dto.stock.StockNewsResponse;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.StockNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockNewsService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DETAIL_NEWS_LIMIT = 10;

    private final StockNewsRepository stockNewsRepository;

    @Transactional
    public void saveForStock(Stock stock, List<StockNewsData> newsItems) {
        if (newsItems == null || newsItems.isEmpty()) {
            return;
        }

        for (StockNewsData item : newsItems) {
            if (item == null || item.url() == null || item.url().isBlank()
                    || item.title() == null || item.title().isBlank()) {
                continue;
            }

            String normalizedUrl = item.url().trim();
            String urlHash = sha256(normalizedUrl);
            StockNews news = stockNewsRepository.findByUrlHash(urlHash)
                    .orElseGet(() -> StockNews.create(
                            item.title().trim(),
                            normalizeNullable(item.summary()),
                            normalizeSource(item.source()),
                            normalizedUrl,
                            urlHash,
                            normalizePublishedAt(item.publishedAt())
                    ));

            news.updateArticle(
                    item.title().trim(),
                    normalizeNullable(item.summary()),
                    normalizeSource(item.source()),
                    normalizedUrl,
                    normalizePublishedAt(item.publishedAt())
            );
            news.linkStock(stock);
            stockNewsRepository.save(news);
        }
    }

    public Page<StockNewsResponse> search(
            String keyword,
            String ticker,
            int page,
            int size
    ) {
        validatePage(page, size);

        Pageable pageable = PageRequest.of(
                page,
                Math.min(size, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "publishedAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Page<StockNews> newsPage = stockNewsRepository.search(
                normalizeSearchValue(keyword),
                normalizeSearchValue(ticker),
                pageable
        );

        if (newsPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, newsPage.getTotalElements());
        }

        List<Long> newsIds = newsPage.getContent().stream()
                .map(StockNews::getId)
                .toList();
        Map<Long, StockNews> newsById = new HashMap<>();
        stockNewsRepository
                .findAllWithRelatedStocksByIdIn(newsIds)
                .forEach(news -> newsById.put(news.getId(), news));

        List<StockNewsResponse> content = newsIds.stream()
                .map(newsById::get)
                .filter(java.util.Objects::nonNull)
                .map(StockNewsResponse::from)
                .toList();

        return new PageImpl<>(content, pageable, newsPage.getTotalElements());
    }

    public List<StockNews> findLatestEntitiesByTicker(String ticker) {
        return stockNewsRepository.findLatestByTicker(
                ticker,
                PageRequest.of(0, DETAIL_NEWS_LIMIT)
        );
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String normalizeSearchValue(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeSource(String value) {
        return value == null || value.isBlank() ? "Unknown" : value.trim();
    }

    private OffsetDateTime normalizePublishedAt(OffsetDateTime value) {
        return value == null ? OffsetDateTime.now(ZoneOffset.UTC) : value;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
