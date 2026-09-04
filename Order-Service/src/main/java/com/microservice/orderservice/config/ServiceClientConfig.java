package com.microservice.orderservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.client.RestClient;

@Configuration
public class ServiceClientConfig {

    @Bean
    @LoadBalanced
    @Qualifier("loadBalancedRestClientBuilder")
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @Primary
    RestClient.Builder eurekaRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @Qualifier("inventoryClient")
    RestClient inventoryClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder,
            @Value("${inventory.service.url}") String inventoryServiceUrl) {
        return restClientBuilder.baseUrl(inventoryServiceUrl).build();
    }

    @Bean
    @Qualifier("notificationClient")
    RestClient notificationClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder,
            @Value("${notification.service.url}") String notificationServiceUrl) {
        return restClientBuilder.baseUrl(notificationServiceUrl).build();
    }
}