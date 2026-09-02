package com.microservice.orderservice.client;

public record NotificationRequest(
        Long orderId,
        String recipient,
        String type,
        String message) {
}