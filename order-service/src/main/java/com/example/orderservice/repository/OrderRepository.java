package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 订单Repository
 */
@Mapper
public interface OrderRepository extends BaseMapper<Order> {
    List<Order> selectByUserId(Long userId);
}