package com.example.userservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户DTO
 */
@Data
public class UserDTO {
    private String id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Integer status;
    private String avatar;
    private Integer balance;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}