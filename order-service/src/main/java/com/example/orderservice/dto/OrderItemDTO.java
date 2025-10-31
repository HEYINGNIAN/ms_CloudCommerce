package com.example.orderservice.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 订单项DTO
 */
@Data
public class OrderItemDTO {
    private String id;
    private String orderId;
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String imageUrl;
}