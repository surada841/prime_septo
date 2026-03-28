package com.revshop.dto.common;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PagedResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private String sortBy;
    private String sortDir;
}
