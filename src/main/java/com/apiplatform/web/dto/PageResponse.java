package com.apiplatform.web.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard pagination envelope for list endpoints that can genuinely grow unbounded
 * (mock traffic logs, request history). Deliberately offset-based (backed by Spring
 * Data's {@code Page}), not cursor-based: cursor pagination earns its complexity once
 * a list is large enough that COUNT-based offset queries get expensive or once
 * concurrent inserts make page drift a real user-visible problem. Neither is true here
 * yet -- this is the simpler, standard, well-tested option, and it's a compatible
 * foundation to swap the underlying query strategy later without changing this shape.
 */
public record PageResponse<T>(
        List<T> data,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
