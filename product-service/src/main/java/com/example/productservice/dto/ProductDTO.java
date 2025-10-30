package com.example.productservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 产品DTO
 */
@Data
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private Integer price;
    private Integer stock;
    private Integer status;
    private String imageUrl;
    private Long categoryId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}