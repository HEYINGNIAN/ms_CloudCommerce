package com.example.userservice.controller;

import com.example.userservice.entity.User;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.service.UserService;
import com.example.common.entity.Result;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 获取用户列表
     */
    @GetMapping
    @CircuitBreaker(name = "userService", fallbackMethod = "getAllUsersFallback")
    public Result<List<UserDTO>> getAllUsers() {
        log.info("获取用户列表");
        return userService.getAllUsers();
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    @RateLimiter(name = "userService", fallbackMethod = "rateLimiterFallback")
    public Result<UserDTO> getUserById(@PathVariable String id) {
        log.info("获取用户: {}", id);
        return userService.getUserById(id);
    }

    /**
     * 创建用户
     */
    @PostMapping
    @CircuitBreaker(name = "userService", fallbackMethod = "createUserFallback")
    public Result<UserDTO> createUser(@RequestBody User user) {
        log.info("创建用户: {}", user.getUsername());
        return userService.createUser(user);
    }

    /**
     * 更新用户
     */
    @PutMapping
    @CircuitBreaker(name = "userService", fallbackMethod = "updateUserFallback")
    public Result<UserDTO> updateUser(@RequestBody User user) {
        log.info("更新用户: {}", user.getId());
        return userService.updateUser(user);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @CircuitBreaker(name = "userService", fallbackMethod = "deleteUserFallback")
    public Result<Boolean> deleteUser(@PathVariable String id) {
        log.info("删除用户: {}", id);
        return userService.deleteUser(id);
    }

    /**
     * 扣减余额
     */
    @PostMapping("/deduct-balance")
    @CircuitBreaker(name = "userService", fallbackMethod = "deductBalanceFallback")
    public Result<Boolean> deductBalance(@RequestParam String userId, @RequestParam Integer amount) {
        log.info("扣减用户余额: userId={}, amount={}", userId, amount);
        return userService.deductBalance(userId, amount);
    }

    // 熔断降级方法
    public Result<List<UserDTO>> getAllUsersFallback(Throwable t) {
        log.error("获取用户列表熔断", t);
        return Result.fail("服务暂时不可用");
    }

    public Result<UserDTO> getUserByIdFallback(String id, Throwable t) {
        log.error("获取用户熔断: {}", id, t);
        return Result.fail("服务暂时不可用");
    }

    public Result<UserDTO> createUserFallback(User user, Throwable t) {
        log.error("创建用户熔断", t);
        return Result.fail("服务暂时不可用");
    }

    public Result<UserDTO> updateUserFallback(User user, Throwable t) {
        log.error("更新用户熔断", t);
        return Result.fail("服务暂时不可用");
    }

    public Result<Boolean> deleteUserFallback(String id, Throwable t) {
        log.error("删除用户熔断: {}", id, t);
        return Result.fail("服务暂时不可用");
    }

    public Result<Boolean> deductBalanceFallback(String userId, Integer amount, Throwable t) {
        log.error("扣减余额熔断: userId={}, amount={}", userId, amount, t);
        return Result.fail("服务暂时不可用");
    }

    public Result<?> rateLimiterFallback(Throwable t) {
        log.warn("接口限流");
        return Result.fail("请求过于频繁，请稍后再试");
    }
}