package com.revshop.dto.admin;

import com.revshop.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminUserResponse {

    private Long id;
    private String email;
    private Role role;
    private Boolean active;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
