package com.revshop.controller;

import com.revshop.dto.common.ApiResponse;
import com.revshop.dto.profile.ProfileResponse;
import com.revshop.dto.profile.UpdateBuyerProfileRequest;
import com.revshop.dto.profile.UpdateSellerProfileRequest;
import com.revshop.service.ProfileService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Buyer and seller profile APIs")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> myProfile(Authentication auth) {
        ProfileResponse response = profileService.getMyProfile(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", response));
    }

    @PutMapping("/buyer")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateBuyerProfile(
            @Valid @RequestBody UpdateBuyerProfileRequest request,
            Authentication auth
    ) {
        ProfileResponse response = profileService.updateBuyerProfile(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Buyer profile updated", response));
    }

    @PutMapping("/seller")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateSellerProfile(
            @Valid @RequestBody UpdateSellerProfileRequest request,
            Authentication auth
    ) {
        ProfileResponse response = profileService.updateSellerProfile(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Seller profile updated", response));
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfileResponse>> uploadProfilePhoto(
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {
        ProfileResponse response = profileService.uploadProfilePhoto(auth.getName(), file);
        return ResponseEntity.ok(ApiResponse.success("Profile photo updated", response));
    }
}
