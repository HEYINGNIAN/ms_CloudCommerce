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

    @GetMapping("/users/{id}")
    Result<UserDTO> getUserById(@PathVariable("id") String id);

    @PostMapping("/users/deduct-balance")
    Result<Boolean> deductBalance(@RequestParam("userId") String userId, @RequestParam("amount") Integer amount);
}