package com.microservice.orderservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ServiceClientConfig {

    @Bean
    @Qualifier("inventoryClient")
    RestClient inventoryClient(@Value("${inventory.service.url}") String inventoryServiceUrl) {
        return RestClient.builder().baseUrl(inventoryServiceUrl).build();
    }

    @Bean
    @Qualifier("notificationClient")
    RestClient notificationClient(@Value("${notification.service.url}") String notificationServiceUrl) {
        return RestClient.builder().baseUrl(notificationServiceUrl).build();
    }
}