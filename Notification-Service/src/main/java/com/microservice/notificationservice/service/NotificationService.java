package com.microservice.notificationservice.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.microservice.notificationservice.entity.Notification;
import com.microservice.notificationservice.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> findAll() {
        return notificationRepository.findAll();
    }

    public Notification findById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    public Notification create(Notification notification) {
        return notificationRepository.save(notification);
    }

    public Notification update(Long id, Notification notification) {
        Notification existingNotification = findById(id);
        existingNotification.setOrderId(notification.getOrderId());
        existingNotification.setRecipient(notification.getRecipient());
        existingNotification.setType(notification.getType());
        existingNotification.setMessage(notification.getMessage());
        existingNotification.setStatus(notification.getStatus());
        return notificationRepository.save(existingNotification);
    }

    public void delete(Long id) {
        notificationRepository.delete(findById(id));
    }
}