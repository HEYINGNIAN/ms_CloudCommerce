package com.example.common.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 用户DTO类
 */
@Data
public class UserDTO {
    private Long id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private BigDecimal balance;
    private Integer status;
}