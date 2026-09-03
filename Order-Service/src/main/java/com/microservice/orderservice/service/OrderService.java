package com.microservice.orderservice.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.microservice.orderservice.entity.Order;
import com.microservice.orderservice.repository.OrderRepository;
import com.microservice.orderservice.client.NotificationRequest;
import com.microservice.orderservice.client.ProductResponse;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestClient inventoryClient;
    private final RestClient notificationClient;

    public OrderService(OrderRepository orderRepository,
            @org.springframework.beans.factory.annotation.Qualifier("inventoryClient") RestClient inventoryClient,
            @org.springframework.beans.factory.annotation.Qualifier("notificationClient") RestClient notificationClient) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.notificationClient = notificationClient;
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    public Order create(Order order) {
        reserveInventory(order);

        ProductResponse productInfo = getProductInfo(order.getProductId());
        if (productInfo.getQuantity() < order.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Insufficient inventory for product " + order.getProductId());
        }
        order.setTotalPrice(
                BigDecimal.valueOf(productInfo.getPrice()).multiply(BigDecimal.valueOf(order.getQuantity())));
        

        Order createdOrder = orderRepository.save(order);
        sendCreationNotification(createdOrder);
        return createdOrder;
    }

    public Order update(Long id, Order order) {

        ProductResponse productInfo = getProductInfo(order.getProductId());
        if (productInfo.getQuantity() < order.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient inventory for product " + order.getProductId());
        }

        Order existingOrder = findById(id);
        existingOrder.setProductId(order.getProductId());
        existingOrder.setCustomerEmail(order.getCustomerEmail());
        existingOrder.setQuantity(order.getQuantity());
        existingOrder.setTotalPrice(BigDecimal.valueOf(productInfo.getPrice()).multiply(BigDecimal.valueOf(order.getQuantity())));
        existingOrder.setStatus(order.getStatus());
        Order order2 =  orderRepository.save(existingOrder);
        updateNotification(order2);
        return order2;
    }

    public void delete(Long id) {
        orderRepository.delete(findById(id));
    }

    private void reserveInventory(Order order) {
        try {
            inventoryClient.post()
                    .uri("/api/products/{id}/reserve", order.getProductId())
                    .body(order.getQuantity())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Unable to reserve inventory: " + exception.getResponseBodyAsString(), exception);
        }
    }

    private ProductResponse getProductInfo(Long productId) {
        try {
            return inventoryClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductResponse.class);
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Unable to reserve inventory: " + exception.getResponseBodyAsString(), exception);
        }
    }

    private void sendCreationNotification(Order order) {
        
        NotificationRequest notification = new NotificationRequest(
                order.getId(),
                order.getCustomerEmail(),
                "EMAIL",
                "Order " + order.getId() + " was created",
                order.getStatus());
        try {
            notificationClient.post()
                    .uri("/api/notifications")
                    .body(notification)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Order created but notification failed", exception);
        }
    }

    private void updateNotification(Order order) {

        NotificationRequest notification = new NotificationRequest(
                order.getId(),
                order.getCustomerEmail(),
                "EMAIL",
                "Order " + order.getId() + " was updated",
                order.getStatus());
        try {
            notificationClient.put()
                    .uri("/api/notifications/"+order.getId())
                    .body(notification)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Order updated but notification failed", exception);
        }
    }
}