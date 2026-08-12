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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Profile("real")
public class AlpacaClient {

    private static final ZoneId NEW_YORK =
            ZoneId.of("America/New_York");
    private static final LocalTime EXTENDED_MARKET_OPEN =
            LocalTime.of(4, 0);

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
        validateTicker(ticker);

        LocalDate tradingDate = LocalDate.now(NEW_YORK);
        Instant start = tradingDate.minusDays(lookbackDays)
                .atStartOfDay(NEW_YORK)
                .toInstant();
        // 오늘 진행 중인 일봉은 제외하고 전일까지 완성된 일봉만 조회한다.
        Instant end = tradingDate.atStartOfDay(NEW_YORK)
                .toInstant()
                .minusNanos(1);

        return getBars(ticker, "1Day", start, end, 1000);
    }

    public AlpacaBarsResponse getTodayMinuteBars(String ticker) {
        validateTicker(ticker);

        ZonedDateTime now = ZonedDateTime.now(NEW_YORK);
        // 프리마켓 개장부터 무료 플랜이 허용하는 지연 시각까지 조회한다.
        Instant start = now.toLocalDate()
                .atTime(EXTENDED_MARKET_OPEN)
                .atZone(NEW_YORK)
                .toInstant();
        Instant end = now.toInstant()
                .minus(dataDelayMinutes, ChronoUnit.MINUTES);

        if (!end.isAfter(start)) {
            throw providerError(
                    "Alpaca delayed data is not available yet today"
            );
        }

        return getBars(ticker, "1Min", start, end, 10000);
    }

    private AlpacaBarsResponse getBars(
            String ticker,
            String timeframe,
            Instant start,
            Instant end,
            int limit
    ) {

        try {
            AlpacaBarsResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stocks/{symbol}/bars")
                            .queryParam("timeframe", timeframe)
                            .queryParam("start", start)
                            .queryParam("end", end)
                            .queryParam("feed", "sip")
                            .queryParam("adjustment", "split")
                            .queryParam("sort", "asc")
                            .queryParam("limit", limit)
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

    private void validateTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException(
                    "조회할 종목 티커는 비어 있을 수 없습니다."
            );
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
