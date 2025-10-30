package com.example.userservice.entity;

import com.example.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private Integer status;
    private String avatar;
    private Integer balance;
}