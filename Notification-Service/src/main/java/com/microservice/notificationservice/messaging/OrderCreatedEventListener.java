package com.microservice.notificationservice.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.microservice.notificationservice.entity.Notification;
import com.microservice.notificationservice.service.NotificationService;


// consumer for order.created event
@Component
public class OrderCreatedEventListener {

    private final NotificationService notificationService;

    public OrderCreatedEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "order.created")
    public void handle(OrderCreatedEvent event) {
        Notification notification = new Notification(
                event.orderId(),
                event.customerEmail(),
                Notification.Type.EMAIL,
                "Order " + event.orderId() + " was created");
        notification.setStatus(Notification.Status.valueOf(event.status()));
        notificationService.create(notification);
    }
}
