package com.example.messageconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 消息消费者启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@RefreshScope
public class MessageConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageConsumerApplication.class, args);
    }

}