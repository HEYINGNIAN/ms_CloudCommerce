package com.example.apigateway.controller;

import com.example.common.entity.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 熔断降级控制器
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    /**
     * 用户服务熔断降级
     */
    @RequestMapping("/user")
    public Mono<Result<?>> userFallback() {
        log.warn("用户服务熔断降级");
        return Mono.just(Result.fail("用户服务暂时不可用，请稍后重试"));
    }

    /**
     * 产品服务熔断降级
     */
    @RequestMapping("/product")
    public Mono<Result<?>> productFallback() {
        log.warn("产品服务熔断降级");
        return Mono.just(Result.fail("产品服务暂时不可用，请稍后重试"));
    }

    /**
     * 订单服务熔断降级
     */
    @RequestMapping("/order")
    public Mono<Result<?>> orderFallback() {
        log.warn("订单服务熔断降级");
        return Mono.just(Result.fail("订单服务暂时不可用，请稍后重试"));
    }
}