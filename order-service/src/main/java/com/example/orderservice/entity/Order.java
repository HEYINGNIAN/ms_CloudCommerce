package com.example.orderservice.entity;

import com.example.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order")
public class Order extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Integer status; // 订单状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消
    private String orderNo;
    private String paymentMethod;
}