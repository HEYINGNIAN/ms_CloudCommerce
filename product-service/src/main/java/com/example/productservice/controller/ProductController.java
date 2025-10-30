package com.example.productservice.controller;

import com.example.productservice.entity.Product;
import com.example.productservice.dto.ProductDTO;
import com.example.productservice.service.ProductService;
import com.example.common.entity.Result;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 产品控制器
 */
@RestController
@RequestMapping("/api/products")
@Slf4j
public class ProductController {

    @Resource
    private ProductService productService;

    /**
     * 获取产品列表
     */
    @GetMapping
    @CircuitBreaker(name = "productService", fallbackMethod = "getAllProductsFallback")
    public Result<List<ProductDTO>> getAllProducts() {
        log.info("获取产品列表");
        return productService.getAllProducts();
    }

    /**
     * 根据ID获取产品
     */
    @GetMapping("/{id}")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductByIdFallback")
    @RateLimiter(name = "productService", fallbackMethod = "rateLimiterFallback")
    public Result<ProductDTO> getProductById(@PathVariable Long id) {
        log.info("获取产品: {}", id);
        return productService.getProductById(id);
    }

    /**
     * 创建产品
     */
    @PostMapping
    @CircuitBreaker(name = "productService", fallbackMethod = "createProductFallback")
    public Result<ProductDTO> createProduct(@RequestBody Product product) {
        log.info("创建产品: {}", product.getName());
        return productService.createProduct(product);
    }

    /**
     * 更新产品
     */
    @PutMapping
    @CircuitBreaker(name = "productService", fallbackMethod = "updateProductFallback")
    public Result<ProductDTO> updateProduct(@RequestBody Product product) {
        log.info("更新产品: {}", product.getId());
        return productService.updateProduct(product);
    }

    /**
     * 删除产品
     */
    @DeleteMapping("/{id}")
    @CircuitBreaker(name = "productService", fallbackMethod = "deleteProductFallback")
    public Result<Boolean> deleteProduct(@PathVariable Long id) {
        log.info("删除产品: {}", id);
        return productService.deleteProduct(id);
    }

    /**
     * 扣减库存
     */
    @PostMapping("/deduct-stock")
    @CircuitBreaker(name = "productService", fallbackMethod = "deductStockFallback")
    public Result<Boolean> deductStock(@RequestParam Long productId, @RequestParam Integer quantity) {
        log.info("扣减产品库存: productId={}, quantity={}", productId, quantity);
        return productService.deductStock(productId, quantity);
    }

    /**
     * 增加库存
     */
    @PostMapping("/add-stock")
    @CircuitBreaker(name = "productService", fallbackMethod = "addStockFallback")
    public Result<Boolean> addStock(@RequestParam Long productId, @RequestParam Integer quantity) {
        log.info("增加产品库存: productId={}, quantity={}", productId, quantity);
        return productService.addStock(productId, quantity);
    }

    // 熔断降级方法
    public Result<List<ProductDTO>> getAllProductsFallback(Throwable t) {
        log.error("获取产品列表熔断", t);
        return Result.fail("服务暂时不可用");
    }

    public Result<ProductDTO> getProductByIdFallback(Long id, Throwable t) {
        log.error("获取产品熔断: {}", id, t);
        return Result.fail("服务暂时不可用");
    }

    public Result<ProductDTO> createProductFallback(Product product, Throwable t) {
        log.error("创建产品熔断", t);
        return Result.fail("服务暂时不可用");
    }

    public Result<ProductDTO> updateProductFallback(Product product, Throwable t) {
        log.error("更新产品熔断", t);
        return Result.fail("服务暂时不可用");
    }

    public Result<Boolean> deleteProductFallback(Long id, Throwable t) {
        log.error("删除产品熔断: {}", id, t);
        return Result.fail("服务暂时不可用");
    }

    public Result<Boolean> deductStockFallback(Long productId, Integer quantity, Throwable t) {
        log.error("扣减库存熔断: productId={}, quantity={}", productId, quantity, t);
        return Result.fail("服务暂时不可用");
    }

    public Result<Boolean> addStockFallback(Long productId, Integer quantity, Throwable t) {
        log.error("增加库存熔断: productId={}, quantity={}", productId, quantity, t);
        return Result.fail("服务暂时不可用");
    }

    public Result<?> rateLimiterFallback(Throwable t) {
        log.warn("接口限流");
        return Result.fail("请求过于频繁，请稍后再试");
    }
}