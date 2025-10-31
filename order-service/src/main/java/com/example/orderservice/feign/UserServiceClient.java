package com.example.orderservice.feign;

import com.example.common.entity.Result;
import com.example.common.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户服务Feign客户端
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    Result<UserDTO> getUserById(@PathVariable("id") Long id);

    @PostMapping("/api/users/deduct-balance")
    Result<Boolean> deductBalance(@RequestParam("userId") Long userId, @RequestParam("amount") Integer amount);
}