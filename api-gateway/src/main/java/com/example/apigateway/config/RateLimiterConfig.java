package com.example.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 限流配置类
 */
@Configuration
public class RateLimiterConfig {

    /**
     * 用户限流键解析器
     * 使用用户IP作为限流的key
     */
    @Bean("userKeyResolver")
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress());
    }
}