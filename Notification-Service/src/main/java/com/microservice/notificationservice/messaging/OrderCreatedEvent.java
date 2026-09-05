package com.microservice.notificationservice.messaging;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        Long orderId,
        String customerEmail,
        BigDecimal totalPrice,
        String status) {
}
