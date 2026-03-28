package com.revshop.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSellerProfileRequest {

    @NotBlank(message = "Business name is required")
    @Size(max = 150, message = "Business name must be <= 150 chars")
    private String businessName;

    @Size(max = 20, message = "GST number must be <= 20 chars")
    private String gstNumber;

    @Size(max = 15, message = "Phone must be <= 15 chars")
    private String phone;

    @Size(max = 255, message = "Business address must be <= 255 chars")
    private String businessAddress;
}
