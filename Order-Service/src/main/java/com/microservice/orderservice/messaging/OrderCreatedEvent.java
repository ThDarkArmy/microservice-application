package com.microservice.orderservice.messaging;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        Long orderId,
        String customerEmail,
        BigDecimal totalPrice,
        String status) {
}
