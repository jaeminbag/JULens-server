package com.julensserver.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Component
@Profile("real")
public class FinnhubClient {

    private final RestClient restClient;
    private final String apiKey;

    public FinnhubClient(
            RestClient.Builder restClientBuilder,
            @Value("${finnhub.base-url:https://finnhub.io/api/v1}")
            String baseUrl,
            @Value("${finnhub.api-key}") String apiKey
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public FinnhubCandleResponse getDailyCandles(
            String ticker,
            int lookbackDays
    ) {
        long from = LocalDate.now(ZoneOffset.UTC)
                .minusDays(lookbackDays)
                .atStartOfDay()
                .toEpochSecond(ZoneOffset.UTC);
        long to = java.time.Instant.now().getEpochSecond();

        try {
            FinnhubCandleResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stock/candle")
                            .queryParam("symbol", ticker)
                            .queryParam("resolution", "D")
                            .queryParam("from", from)
                            .queryParam("to", to)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubCandleResponse.class);

            if (response == null) {
                throw providerError("Finnhub candle response is empty");
            }
            return response;
        } catch (RestClientException exception) {
            throw providerError("Finnhub candle request failed", exception);
        }
    }

    public List<FinnhubNewsItem> getCompanyNews(
            String ticker,
            int lookbackDays
    ) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        try {
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

    private BusinessException providerError(String detail) {
        return new BusinessException(
                ErrorCode.EXTERNAL_DATA_PROVIDER_ERROR,
                detail
        );
    }

    private BusinessException providerError(
            String detail,
            RuntimeException cause
    ) {
        BusinessException exception = providerError(detail);
        exception.initCause(cause);
        return exception;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinnhubCandleResponse(
            @JsonProperty("c") List<BigDecimal> closes,
            @JsonProperty("v") List<BigDecimal> volumes,
            @JsonProperty("s") String status,
            @JsonProperty("error") String error
    ) {
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
}
