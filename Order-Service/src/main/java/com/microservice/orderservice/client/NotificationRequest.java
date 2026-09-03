package com.microservice.orderservice.client;

import com.microservice.orderservice.entity.Order.Status;

public record NotificationRequest(
        Long orderId,
        String recipient,
        String type,
        String message,
        Status status) {

}