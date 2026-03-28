package com.revshop.dto.seller;

import com.revshop.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class SellerRecentOrderResponse {

    private Long orderId;
    private String orderNumber;
    private String buyerEmail;
    private OrderStatus orderStatus;
    private Integer itemCount;
    private BigDecimal orderAmountForSeller;
    private LocalDateTime orderedAt;
}
