package com.enterprise.ems.service.hiring;

import com.enterprise.ems.dto.PageResponse;
import org.springframework.data.domain.Page;

import java.util.function.Function;

/* PURPOSE: Spring Data Page -> the app's PageResponse, mapping each row on the way. */
public final class Pages {

    private Pages() {}

    public static <E, D> PageResponse<D> map(Page<E> page, Function<E, D> mapper) {
        return PageResponse.<D>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
