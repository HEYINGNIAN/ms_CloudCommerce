package com.example.orderservice.repository;

import com.example.orderservice.entity.OrderItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 订单项Repository
 */
@Mapper
public interface OrderItemRepository extends BaseMapper<OrderItem> {
    List<OrderItem> selectByOrderId(String orderId);
}
