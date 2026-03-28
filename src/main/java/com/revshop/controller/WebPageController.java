package com.revshop.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class WebPageController {

    @GetMapping({"/", "/home"})
    public String homePage() {
        log.info("Serving home page");
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        log.info("Serving login page");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        log.info("Serving register page");
        return "auth/register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        log.info("Serving forgot password page");
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage() {
        log.info("Serving reset password page");
        return "auth/reset-password";
    }

    @GetMapping("/buyer/dashboard")
    public String buyerDashboardPage() {
        log.info("Serving buyer dashboard page");
        return "buyer/dashboard";
    }

    @GetMapping("/buyer/cart")
    public String buyerCartPage() {
        log.info("Serving buyer cart page");
        return "buyer/cart";
    }

    @GetMapping("/buyer/orders")
    public String buyerOrdersPage() {
        log.info("Serving buyer orders page");
        return "buyer/orders";
    }

    @GetMapping("/buyer/payment-success")
    public String buyerPaymentSuccessPage() {
        log.info("Serving buyer payment success page");
        return "buyer/payment-success";
    }

    @GetMapping("/buyer/payment-cancel")
    public String buyerPaymentCancelPage() {
        log.info("Serving buyer payment cancel page");
        return "buyer/payment-cancel";
    }

    @GetMapping("/buyer/wishlist")
    public String buyerWishlistPage() {
        log.info("Serving buyer wishlist page");
        return "buyer/wishlist";
    }

    @GetMapping("/buyer/notifications")
    public String buyerNotificationsPage() {
        log.info("Serving buyer notifications page");
        return "buyer/notifications";
    }

    @GetMapping("/buyer/profile")
    public String buyerProfilePage() {
        log.info("Serving buyer profile page");
        return "buyer/profile";
    }

    @GetMapping("/seller/dashboard")
    public String sellerDashboardPage() {
        log.info("Serving seller dashboard page");
        return "seller/dashboard";
    }

    @GetMapping("/seller/orders")
    public String sellerOrdersPage() {
        log.info("Serving seller orders page");
        return "seller/orders";
    }

    @GetMapping("/seller/products")
    public String sellerProductsPage() {
        log.info("Serving seller products page");
        return "seller/products";
    }

    @GetMapping("/seller/categories")
    public String sellerCategoriesPage() {
        log.info("Serving seller categories page");
        return "seller/categories";
    }

    @GetMapping("/seller/notifications")
    public String sellerNotificationsPage() {
        log.info("Serving seller notifications page");
        return "seller/notifications";
    }

    @GetMapping("/seller/profile")
    public String sellerProfilePage() {
        log.info("Serving seller profile page");
        return "seller/profile";
    }
}
