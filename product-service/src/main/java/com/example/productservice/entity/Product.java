package com.example.productservice.entity;

import com.example.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产品实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product")
public class Product extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private Integer price;
    private Integer stock;
    private Integer status;
    private String imageUrl;
    private String categoryId;
}