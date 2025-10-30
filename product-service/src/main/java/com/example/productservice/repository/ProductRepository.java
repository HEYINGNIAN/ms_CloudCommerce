package com.example.productservice.repository;

import com.example.productservice.entity.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品Repository
 */
@Mapper
public interface ProductRepository extends BaseMapper<Product> {
}