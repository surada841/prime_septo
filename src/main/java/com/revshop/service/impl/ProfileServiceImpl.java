package com.revshop.service.impl;

import com.revshop.dao.UserDAO;
import com.revshop.dto.profile.ProfileResponse;
import com.revshop.dto.profile.UpdateBuyerProfileRequest;
import com.revshop.dto.profile.UpdateSellerProfileRequest;
import com.revshop.entity.BuyerProfile;
import com.revshop.entity.Role;
import com.revshop.entity.SellerProfile;
import com.revshop.entity.User;
import com.revshop.exception.BadRequestException;
import com.revshop.exception.ForbiddenOperationException;
import com.revshop.exception.InternalServerException;
import com.revshop.exception.ResourceNotFoundException;
import com.revshop.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final long MAX_PROFILE_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    private final UserDAO userDAO;
    @Value("${app.upload.profile-images-dir:uploads/profile-images}")
    private String profileImagesDir;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(String email) {
        User user = getActiveUser(email);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateBuyerProfile(String email, UpdateBuyerProfileRequest request) {
        User user = getActiveUser(email);
        if (user.getRole() != Role.BUYER) {
            throw new ForbiddenOperationException("Only buyer can update buyer profile");
        }

        BuyerProfile profile = user.getBuyerProfile();
        if (profile == null) {
            profile = BuyerProfile.builder().user(user).build();
            user.setBuyerProfile(profile);
        }

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());

        userDAO.update(user);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateSellerProfile(String email, UpdateSellerProfileRequest request) {
        User user = getActiveUser(email);
        if (user.getRole() != Role.SELLER) {
            throw new ForbiddenOperationException("Only seller can update seller profile");
        }

        SellerProfile profile = user.getSellerProfile();
        if (profile == null) {
            profile = SellerProfile.builder().user(user).build();
            user.setSellerProfile(profile);
        }

        profile.setBusinessName(request.getBusinessName());
        profile.setGstNumber(request.getGstNumber());
        profile.setPhone(request.getPhone());
        profile.setBusinessAddress(request.getBusinessAddress());

        userDAO.update(user);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse uploadProfilePhoto(String email, MultipartFile file) {
        User user = getActiveUser(email);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Profile image file is required");
        }
        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE_BYTES) {
            throw new BadRequestException("Profile image is too large. Max allowed size is 5MB");
        }

        String originalFilename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        if (!isImageFile(file, extension)) {
            throw new BadRequestException("Only image files are allowed");
        }

        String safeExtension = extension.isBlank() ? ".jpg" : extension;
        String fileName = "profile-" + user.getId() + "-" + UUID.randomUUID() + safeExtension;
        Path uploadDir = Paths.get(profileImagesDir).toAbsolutePath().normalize();
        Path destination = uploadDir.resolve(fileName).normalize();

        try {
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new InternalServerException("Failed to store profile image");
        }

        deleteExistingProfileImageIfAny(user.getProfileImageUrl(), uploadDir);
        user.setProfileImageUrl(buildPublicImageUrl(fileName));
        userDAO.update(user);
        return mapToProfileResponse(user);
    }

    private User getActiveUser(String email) {
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new ForbiddenOperationException("User account is inactive");
        }
        return user;
    }

    private ProfileResponse mapToProfileResponse(User user) {
        BuyerProfile buyer = user.getBuyerProfile();
        SellerProfile seller = user.getSellerProfile();

        return ProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .profileImageUrl(normalizePublicImageUrl(user.getProfileImageUrl()))
                .firstName(buyer == null ? null : buyer.getFirstName())
                .lastName(buyer == null ? null : buyer.getLastName())
                .phone(buyer != null ? buyer.getPhone() : (seller == null ? null : seller.getPhone()))
                .address(buyer == null ? null : buyer.getAddress())
                .businessName(seller == null ? null : seller.getBusinessName())
                .gstNumber(seller == null ? null : seller.getGstNumber())
                .businessAddress(seller == null ? null : seller.getBusinessAddress())
                .build();
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private boolean isImageFile(MultipartFile file, String extension) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        return switch (extension) {
            case ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".avif", ".svg" -> true;
            default -> false;
        };
    }

    private void deleteExistingProfileImageIfAny(String imageUrl, Path uploadDir) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        int index = imageUrl.lastIndexOf('/');
        if (index < 0 || index == imageUrl.length() - 1) {
            return;
        }
        String fileName = imageUrl.substring(index + 1);
        Path target = uploadDir.resolve(fileName).normalize();
        if (!target.startsWith(uploadDir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Keep DB consistency if deleting old file fails.
        }
    }

    private String buildPublicImageUrl(String fileName) {
        return "/uploads/profile-images/" + fileName;
    }

    private String normalizePublicImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        int markerIndex = rawUrl.indexOf("/uploads/");
        if (markerIndex >= 0) {
            return rawUrl.substring(markerIndex);
        }
        return rawUrl;
    }
}
