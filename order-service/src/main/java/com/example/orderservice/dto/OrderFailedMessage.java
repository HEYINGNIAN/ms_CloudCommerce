package com.example.orderservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单失败消息
 */
@Data
public class OrderFailedMessage {
    private String userId;
    private String productId;
    private Integer quantity;
    private String orderNo;
    private String errorMsg;
    private LocalDateTime createTime;
}