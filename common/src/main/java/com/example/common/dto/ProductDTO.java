package com.example.common.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 产品DTO类
 */
@Data
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Integer status;
}