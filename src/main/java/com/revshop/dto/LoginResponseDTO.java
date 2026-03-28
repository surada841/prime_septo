package com.revshop.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDTO {

    private String token;
    private String role;
}