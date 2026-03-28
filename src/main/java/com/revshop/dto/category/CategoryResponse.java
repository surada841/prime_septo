package com.revshop.dto.category;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private String parentName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
