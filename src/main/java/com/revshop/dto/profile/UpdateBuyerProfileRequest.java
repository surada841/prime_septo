package com.revshop.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBuyerProfileRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 80, message = "First name must be <= 80 chars")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 80, message = "Last name must be <= 80 chars")
    private String lastName;

    @Size(max = 15, message = "Phone must be <= 15 chars")
    private String phone;

    @Size(max = 255, message = "Address must be <= 255 chars")
    private String address;
}
