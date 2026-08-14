package com.julensserver.dto.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PageResponseTest {

    @Test
    void PageImpl을_안정적인_페이지_DTO로_변환한다() {
        PageResponse<String> response = PageResponse.from(
                new PageImpl<>(
                        List.of("A", "B"),
                        PageRequest.of(1, 2),
                        5
                )
        );

        assertEquals(List.of("A", "B"), response.content());
        assertEquals(3, response.totalPages());
        assertEquals(5, response.totalElements());
        assertEquals(1, response.number());
        assertFalse(response.first());
    }
}
