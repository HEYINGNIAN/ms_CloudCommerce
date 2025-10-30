package com.example.messageconsumer.feign;

import com.example.common.entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 订单服务Feign客户端
 */
@FeignClient(name = "order-service", fallbackFactory = OrderServiceFallbackFactory.class)
public interface OrderServiceClient {

    /**
     * 更新订单状态
     */
    @PutMapping("/api/orders/status")
    Result<Boolean> updateOrderStatus(@RequestParam Long orderId, @RequestParam Integer status);
}