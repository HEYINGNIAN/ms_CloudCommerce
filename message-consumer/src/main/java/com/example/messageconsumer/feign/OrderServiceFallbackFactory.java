package com.example.messageconsumer.feign;

import com.example.common.entity.Result;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单服务Feign客户端熔断降级工厂
 */
@Component
@Slf4j
public class OrderServiceFallbackFactory implements FallbackFactory<OrderServiceClient> {

    @Override
    public OrderServiceClient create(Throwable cause) {
        log.error("订单服务调用失败: {}", cause.getMessage(), cause);
        return new OrderServiceClient() {
            @Override
            public Result<Boolean> updateOrderStatus(Long orderId, Integer status) {
                log.warn("更新订单状态熔断: orderId={}, status={}", orderId, status);
                return Result.fail("订单服务暂时不可用，无法更新订单状态");
            }
        };
    }
}