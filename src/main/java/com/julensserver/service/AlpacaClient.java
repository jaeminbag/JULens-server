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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Profile("real")
public class AlpacaClient {

    private final RestClient restClient;
    private final String apiKeyId;
    private final String secretKey;
    private final int dataDelayMinutes;

    public AlpacaClient(
            RestClient.Builder restClientBuilder,
            @Value("${alpaca.base-url:https://data.alpaca.markets/v2}")
            String baseUrl,
            @Value("${alpaca.api-key-id}") String apiKeyId,
            @Value("${alpaca.secret-key}") String secretKey,
            @Value("${alpaca.data-delay-minutes:16}")
            int dataDelayMinutes
    ) {
        if (dataDelayMinutes < 15) {
            throw new IllegalArgumentException(
                    "Alpaca 무료 SIP 데이터 지연은 15분 이상이어야 합니다."
            );
        }

        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKeyId = apiKeyId;
        this.secretKey = secretKey;
        this.dataDelayMinutes = dataDelayMinutes;
    }

    public AlpacaBarsResponse getDailyBars(
            String ticker,
            int lookbackDays
    ) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException(
                    "조회할 종목 티커는 비어 있을 수 없습니다."
            );
        }

        LocalDate start = LocalDate.now(ZoneOffset.UTC)
                .minusDays(lookbackDays);
        Instant end = Instant.now()
                .minus(dataDelayMinutes, ChronoUnit.MINUTES);

        try {
            AlpacaBarsResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stocks/{symbol}/bars")
                            .queryParam("timeframe", "1Day")
                            .queryParam("start", start)
                            .queryParam("end", end)
                            .queryParam("feed", "sip")
                            .queryParam("adjustment", "split")
                            .queryParam("sort", "asc")
                            .queryParam("limit", 1000)
                            .build(ticker.trim().toUpperCase()))
                    .header("APCA-API-KEY-ID", apiKeyId)
                    .header("APCA-API-SECRET-KEY", secretKey)
                    .retrieve()
                    .body(AlpacaBarsResponse.class);

            if (response == null) {
                throw providerError("Alpaca bars response is empty");
            }
            return response;
        } catch (RestClientException exception) {
            throw providerError("Alpaca bars request failed", exception);
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
    public record AlpacaBarsResponse(
            List<AlpacaBar> bars,
            String symbol,
            @JsonProperty("next_page_token") String nextPageToken
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlpacaBar(
            @JsonProperty("c") BigDecimal close,
            @JsonProperty("v") BigDecimal volume,
            @JsonProperty("t") Instant timestamp
    ) {
    }
}
