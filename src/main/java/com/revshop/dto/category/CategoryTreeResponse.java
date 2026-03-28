package com.revshop.dto.category;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CategoryTreeResponse {

    private Long id;
    private String name;
    private String description;
    private List<CategoryTreeResponse> children;
}
