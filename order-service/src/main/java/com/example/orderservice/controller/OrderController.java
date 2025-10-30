package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderDTO;
import com.example.orderservice.service.OrderService;
import com.example.common.entity.Result;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping
    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    public Result<OrderDTO> createOrder(@RequestParam Long userId, @RequestParam Long productId, @RequestParam Integer quantity) {
        log.info("创建订单: userId={}, productId={}, quantity={}", userId, productId, quantity);
        return orderService.createOrder(userId, productId, quantity);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrderByIdFallback")
    @RateLimiter(name = "orderService", fallbackMethod = "rateLimiterFallback")
    public Result<OrderDTO> getOrderById(@PathVariable Long id) {
        log.info("获取订单详情: {}", id);
        return orderService.getOrderById(id);
    }

    /**
     * 获取用户订单列表
     */
    @GetMapping("/user/{userId}")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getUserOrdersFallback")
    public Result<List<OrderDTO>> getUserOrders(@PathVariable Long userId) {
        log.info("获取用户订单列表: userId={}", userId);
        return orderService.getUserOrders(userId);
    }

    /**
     * 更新订单状态
     */
    @PutMapping("/status")
    @CircuitBreaker(name = "orderService", fallbackMethod = "updateOrderStatusFallback")
    public Result<Boolean> updateOrderStatus(@RequestParam Long orderId, @RequestParam Integer status) {
        log.info("更新订单状态: orderId={}, status={}", orderId, status);
        return orderService.updateOrderStatus(orderId, status);
    }

    // 熔断降级方法
    public Result<OrderDTO> createOrderFallback(Long userId, Long productId, Integer quantity, Throwable t) {
        log.error("创建订单熔断: userId={}, productId={}, quantity={}", userId, productId, quantity, t);
        return Result.fail("服务暂时不可用，请稍后重试");
    }

    public Result<OrderDTO> getOrderByIdFallback(Long id, Throwable t) {
        log.error("获取订单详情熔断: {}", id, t);
        return Result.fail("服务暂时不可用，请稍后重试");
    }

    public Result<List<OrderDTO>> getUserOrdersFallback(Long userId, Throwable t) {
        log.error("获取用户订单列表熔断: userId={}", userId, t);
        return Result.fail("服务暂时不可用，请稍后重试");
    }

    public Result<Boolean> updateOrderStatusFallback(Long orderId, Integer status, Throwable t) {
        log.error("更新订单状态熔断: orderId={}, status={}", orderId, status, t);
        return Result.fail("服务暂时不可用，请稍后重试");
    }

    public Result<?> rateLimiterFallback(Throwable t) {
        log.warn("接口限流");
        return Result.fail("请求过于频繁，请稍后再试");
    }
}