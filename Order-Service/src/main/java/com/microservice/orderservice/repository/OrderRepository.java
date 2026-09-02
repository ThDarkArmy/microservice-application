package com.microservice.orderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.microservice.orderservice.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}