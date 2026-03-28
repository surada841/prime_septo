package com.revshop.dto;

import com.revshop.entity.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private Role role;
    private Boolean active;
}