package com.example.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;

/**
 * API网关启动类
 */
@SpringBootApplication(scanBasePackages = {"com.example.apigateway", "com.example.common"}, exclude = {org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class, org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class})
@EnableDiscoveryClient
@RefreshScope
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}