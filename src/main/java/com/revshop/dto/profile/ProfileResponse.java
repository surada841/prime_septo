package com.revshop.dto.profile;

import com.revshop.entity.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileResponse {

    private Long userId;
    private String email;
    private Role role;
    private Boolean active;
    private String profileImageUrl;

    private String firstName;
    private String lastName;
    private String phone;
    private String address;

    private String businessName;
    private String gstNumber;
    private String businessAddress;
}
