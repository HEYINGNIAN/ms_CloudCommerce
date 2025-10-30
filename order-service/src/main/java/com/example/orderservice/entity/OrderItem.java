package com.example.orderservice.entity;

import com.example.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单项实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_item")
public class OrderItem extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String imageUrl;
}