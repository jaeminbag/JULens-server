package com.julensserver.service;

public record StockSyncResult(
        int received,
        int activated,
        int created,
        int updated,
        int deactivated
) {
}
