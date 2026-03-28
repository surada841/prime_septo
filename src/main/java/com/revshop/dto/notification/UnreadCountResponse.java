package com.revshop.dto.notification;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnreadCountResponse {

    private Long unreadCount;
}
