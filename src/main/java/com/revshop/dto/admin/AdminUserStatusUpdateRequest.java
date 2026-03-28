package com.revshop.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserStatusUpdateRequest {

    @NotNull(message = "Active is required")
    private Boolean active;
}
