package com.julensserver.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring Data 내부 PageImpl 구조를 외부 API에 직접 노출하지 않는 페이지 DTO다.
 * 기존 프론트가 사용하는 content, totalPages, totalElements 필드는 유지한다.
 */
public record PageResponse<T>(
        List<T> content,
        int totalPages,
        long totalElements,
        int size,
        int number,
        boolean first,
        boolean last,
        int numberOfElements,
        boolean empty
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                List.copyOf(page.getContent()),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber(),
                page.isFirst(),
                page.isLast(),
                page.getNumberOfElements(),
                page.isEmpty()
        );
    }
}
