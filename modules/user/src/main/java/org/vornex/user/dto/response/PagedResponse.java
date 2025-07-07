package org.vornex.user.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
