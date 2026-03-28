package com.revshop.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryCreateRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must be <= 100 chars")
    private String name;

    @Size(max = 500, message = "Description must be <= 500 chars")
    private String description;

    private Long parentId;
}
