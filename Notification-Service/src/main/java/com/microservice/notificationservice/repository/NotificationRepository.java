package com.microservice.notificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.microservice.notificationservice.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}