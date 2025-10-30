package com.example.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * 网关配置类
 */
@Configuration
public class GatewayConfig {

    /**
     * 用户限流Key解析器
     * 使用请求中的user-id作为限流键，如果没有则使用IP地址
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("user-id");
            if (userId != null) {
                return Mono.just(userId);
            }
            // 使用IP地址作为限流键
            String ip = exchange.getRequest().getRemoteAddress().getHostName();
            return Mono.just(ip);
        };
    }
}