package com.revshop.mapper;

import com.revshop.dto.UserResponse;
import com.revshop.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .build();
    }
}
