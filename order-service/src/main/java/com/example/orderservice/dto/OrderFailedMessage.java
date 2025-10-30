package com.example.orderservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单失败消息
 */
@Data
public class OrderFailedMessage {
    private Long userId;
    private Long productId;
    private Integer quantity;
    private String orderNo;
    private String errorMsg;
    private LocalDateTime createTime;
}