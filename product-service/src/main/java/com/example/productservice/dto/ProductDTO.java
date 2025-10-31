package com.example.productservice.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 产品DTO
 */
@Data
public class ProductDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String name;
    private String description;
    private Integer price;
    private Integer stock;
    private Integer status;
    private String imageUrl;
    private String categoryId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}