package com.revshop.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "razorpay")
public class RazorpayProperties {
    private String keyId;
    private String keySecret;
    private String currency = "INR";
    private String successUrl = "http://localhost:8080/buyer/payment-success";
    private String cancelUrl = "http://localhost:8080/buyer/payment-cancel";
    private String companyName = "RevShop";
    private String companyLogo = "";
    private String themeColor = "#0d6efd";

    public boolean isConfigured() {
        return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }
}
