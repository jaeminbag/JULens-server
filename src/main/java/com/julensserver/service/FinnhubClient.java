package com.julensserver.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Profile("real")
public class FinnhubClient {

    private final RestClient restClient;
    private final String apiKey;
    private final long minRequestIntervalNanos;
    private final Object requestRateLock = new Object();
    private long nextRequestAtNanos;

    public FinnhubClient(
            RestClient.Builder restClientBuilder,
            @Value("${finnhub.base-url:https://finnhub.io/api/v1}")
            String baseUrl,
            @Value("${finnhub.api-key}") String apiKey,
            @Value("${finnhub.min-request-interval-millis:1100}")
            long minRequestIntervalMillis
    ) {
        if (minRequestIntervalMillis < 0) {
            throw new IllegalArgumentException(
                    "Finnhub 요청 간격은 0 이상이어야 합니다."
            );
        }

        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.minRequestIntervalNanos = TimeUnit.MILLISECONDS.toNanos(
                minRequestIntervalMillis
        );
    }

    public List<FinnhubNewsItem> getCompanyNews(
            String ticker,
            int lookbackDays
    ) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        try {
            awaitRequestSlot();
            FinnhubNewsItem[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/company-news")
                            .queryParam("symbol", ticker)
                            .queryParam("from", today.minusDays(lookbackDays))
                            .queryParam("to", today)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubNewsItem[].class);

            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientException exception) {
            throw providerError("Finnhub news request failed", exception);
        }
    }

    public List<FinnhubStockSymbol> getStockSymbols(String exchange) {
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalArgumentException(
                    "거래소 코드는 비어 있을 수 없습니다."
            );
        }

        try {
            awaitRequestSlot();
            FinnhubStockSymbol[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stock/symbol")
                            .queryParam("exchange", exchange.trim())
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubStockSymbol[].class);

            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientException exception) {
            throw providerError(
                    "Finnhub stock symbol request failed",
                    exception
            );
        }
    }

    private void awaitRequestSlot() {
        synchronized (requestRateLock) {
            long waitNanos = nextRequestAtNanos - System.nanoTime();
            if (waitNanos > 0) {
                try {
                    TimeUnit.NANOSECONDS.sleep(waitNanos);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw providerError(
                            "Finnhub request was interrupted",
                            exception
                    );
                }
            }
            nextRequestAtNanos = System.nanoTime()
                    + minRequestIntervalNanos;
        }
    }

    private BusinessException providerError(String detail) {
        return new BusinessException(
                ErrorCode.EXTERNAL_DATA_PROVIDER_ERROR,
                detail
        );
    }

    private BusinessException providerError(
            String detail,
            Throwable cause
    ) {
        BusinessException exception = providerError(detail);
        exception.initCause(cause);
        return exception;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinnhubNewsItem(
            String headline,
            String summary,
            String source,
            String url,
            Long datetime
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinnhubStockSymbol(
            String currency,
            String description,
            String displaySymbol,
            String mic,
            String symbol,
            String type
    ) {
    }
}
