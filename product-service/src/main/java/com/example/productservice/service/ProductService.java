package com.example.productservice.service;

import com.example.productservice.entity.Product;
import com.example.productservice.dto.ProductDTO;
import com.example.common.enumz.ErrorCode;
import com.example.common.util.RedissonLockUtil;
import com.example.common.entity.Result;
import com.example.productservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;
import org.redisson.api.RBucket;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.seata.spring.annotation.GlobalTransactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

/**
 * 产品服务
 */
@Service
@Slf4j
public class ProductService {

    @Resource
    private ProductRepository productRepository;

    @Resource
    private RedissonLockUtil redisLockUtil;

    @Resource
    private RedissonClient redissonClient;
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PRODUCT_CACHE_PREFIX = "product:info:";
    private static final String STOCK_CACHE_PREFIX = "product:stock:";

    /**
     * 根据ID获取产品
     */
    public Result<ProductDTO> getProductById(String id) {
        // 先从缓存获取
        String cacheKey = PRODUCT_CACHE_PREFIX + id;
        ProductDTO cachedProduct = (ProductDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            return Result.success(cachedProduct);
        }

        // 缓存未命中，从数据库获取
        Product product = productRepository.selectById(id);
        if (product == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), "产品不存在");
        }

        ProductDTO productDTO = new ProductDTO();
        BeanUtils.copyProperties(product, productDTO);

        // 放入缓存，设置过期时间5分钟
        redisTemplate.opsForValue().set(cacheKey, productDTO, 5, TimeUnit.MINUTES);

        return Result.success(productDTO);
    }

    /**
     * 获取所有产品
     */
    public Result<List<ProductDTO>> getAllProducts() {
        List<Product> products = productRepository.selectList(null);
        List<ProductDTO> productDTOs = products.stream().map(product -> {
            ProductDTO dto = new ProductDTO();
            BeanUtils.copyProperties(product, dto);
            return dto;
        }).collect(Collectors.toList());
        return Result.success(productDTOs);
    }

    /**
     * 创建产品
     */
    public Result<ProductDTO> createProduct(Product product) {
        boolean success = productRepository.insert(product) > 0;
        if (success) {
            ProductDTO productDTO = new ProductDTO();
            BeanUtils.copyProperties(product, productDTO);
            return Result.success(productDTO);
        }
        return Result.fail("创建产品失败");
    }

    /**
     * 更新产品
     */
    public Result<ProductDTO> updateProduct(Product product) {
        boolean success = productRepository.updateById(product) > 0;
        if (success) {
            // 清除缓存
            String cacheKey = PRODUCT_CACHE_PREFIX + product.getId();
            redissonClient.getBucket(cacheKey).delete();
            
            ProductDTO productDTO = new ProductDTO();
            BeanUtils.copyProperties(product, productDTO);
            return Result.success(productDTO);
        }
        return Result.fail("更新产品失败");
    }

    /**
     * 删除产品
     */
    public Result<Boolean> deleteProduct(String id) {
        boolean success = productRepository.deleteById(id) > 0;
        if (success) {
            // 清除缓存
            String cacheKey = PRODUCT_CACHE_PREFIX + id;
            redissonClient.getBucket(cacheKey).delete();
        }
        return success ? Result.success(true) : Result.fail("删除产品失败");
    }

    /**
     * 扣减库存（使用分布式锁和Redis缓存优化）
     */
    @GlobalTransactional
    public Result<Boolean> deductStock(String productId, Integer quantity) {
        // 生成锁的key
        String lockKey = "deduct_stock:" + productId;
        RLock lock = null;
        try {
            // 尝试获取分布式锁
            lock = redisLockUtil.tryLock(lockKey, 10, 3);
            if (lock == null) {
                log.warn("获取分布式锁失败: {}", lockKey);
                return Result.fail(ErrorCode.LOCK_FAIL.getCode(), "获取分布式锁失败");
            }

            // 先检查Redis中的库存
            String stockKey = STOCK_CACHE_PREFIX + productId;
            RBucket<Integer> stockBucket = redissonClient.getBucket(stockKey);
            Integer cachedStock = stockBucket.get();
            if (cachedStock != null && cachedStock < quantity) {
                return Result.fail("库存不足");
            }

            // 从数据库获取产品信息
            Product product = productRepository.selectById(productId);
            if (product == null) {
                return Result.fail(ErrorCode.NOT_FOUND.getCode(), "产品不存在");
            }

            // 检查库存是否充足
            if (product.getStock() < quantity) {
                return Result.fail("库存不足");
            }

            // 扣减库存
            product.setStock(product.getStock() - quantity);
            boolean success = productRepository.updateById(product) > 0;
            if (success) {
                // 更新缓存
                redissonClient.getBucket(stockKey).set(product.getStock());
                // 清除产品缓存，下次查询会重新加载最新数据
                String productCacheKey = PRODUCT_CACHE_PREFIX + productId;
                redissonClient.getBucket(productCacheKey).delete();
                return Result.success(true);
            }
            return Result.fail("扣减库存失败");
        } finally {
            // 释放锁
            redisLockUtil.unlock(lock);
        }
    }

    /**
     * 增加库存
     */
    @Transactional
    public Result<Boolean> addStock(String productId, Integer quantity) {
        Product product = productRepository.selectById(productId);
        if (product == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), "产品不存在");
        }

        product.setStock(product.getStock() + quantity);
        boolean success = productRepository.updateById(product) > 0;
        if (success) {
            // 更新缓存
            String stockKey = STOCK_CACHE_PREFIX + productId;
            redissonClient.getBucket(stockKey).set(product.getStock());
            // 清除产品缓存
            String productCacheKey = PRODUCT_CACHE_PREFIX + productId;
            redissonClient.getBucket(productCacheKey).delete();
        }
        return success ? Result.success(true) : Result.fail("增加库存失败");
    }
}