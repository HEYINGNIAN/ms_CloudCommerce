package com.example.orderservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单DTO
 */
@Data
public class OrderDTO {
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Integer status;
    private String orderNo;
    private String paymentMethod;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<OrderItemDTO> orderItems;
}
