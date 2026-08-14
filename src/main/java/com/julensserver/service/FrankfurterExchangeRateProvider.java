package com.julensserver.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.julensserver.dto.stock.ExchangeRateResponse;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Frankfurter의 일일 기준환율을 조회하고 6시간 동안 메모리에 보관한다.
 * 일시적인 외부 장애가 생기면 마지막 성공 환율을 계속 제공한다.
 */
@Component
@Profile("real")
public class FrankfurterExchangeRateProvider
        implements ExchangeRateProvider {

    private static final Duration CACHE_DURATION = Duration.ofHours(6);

    private final RestClient restClient;
    private volatile CachedRate cachedRate;

    public FrankfurterExchangeRateProvider(
            RestClient.Builder restClientBuilder,
            @Value("${exchange-rate.base-url:https://api.frankfurter.dev/v2}")
            String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public ExchangeRateResponse getUsdKrwRate() {
        CachedRate current = cachedRate;
        if (isFresh(current)) {
            return current.response();
        }
        return refreshOrUseCached(current);
    }

    private synchronized ExchangeRateResponse refreshOrUseCached(
            CachedRate previous
    ) {
        if (isFresh(cachedRate)) {
            return cachedRate.response();
        }

        try {
            FrankfurterRate response = restClient.get()
                    .uri("/rate/USD/KRW")
                    .retrieve()
                    .body(FrankfurterRate.class);
            if (response == null
                    || response.rate() == null
                    || response.rate().signum() <= 0) {
                throw providerError("USD/KRW exchange rate response is empty");
            }

            ExchangeRateResponse result = new ExchangeRateResponse(
                    "USD",
                    "KRW",
                    response.rate(),
                    response.date()
            );
            cachedRate = new CachedRate(result, Instant.now());
            return result;
        } catch (RestClientException | BusinessException exception) {
            CachedRate fallback = cachedRate != null ? cachedRate : previous;
            if (fallback != null) {
                return fallback.response();
            }
            throw providerError("USD/KRW exchange rate request failed", exception);
        }
    }

    private boolean isFresh(CachedRate value) {
        return value != null
                && value.fetchedAt().plus(CACHE_DURATION).isAfter(Instant.now());
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

    private record CachedRate(
            ExchangeRateResponse response,
            Instant fetchedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FrankfurterRate(
            LocalDate date,
            String base,
            String quote,
            BigDecimal rate
    ) {
    }
}
