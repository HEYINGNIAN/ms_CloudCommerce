package com.example.orderservice.service;

import com.example.orderservice.config.RabbitMQConfig;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.dto.OrderDTO;
import com.example.orderservice.dto.OrderItemDTO;
import com.example.orderservice.dto.OrderFailedMessage;
import com.example.orderservice.feign.UserServiceClient;
import com.example.orderservice.feign.ProductServiceClient;
import com.example.common.entity.Result;
import com.example.common.enumz.ErrorCode;
import com.example.common.util.RedissonLockUtil;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.OrderItemRepository;
import com.example.common.dto.ProductDTO;
import com.example.common.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.seata.spring.annotation.GlobalTransactional;

import jakarta.annotation.Resource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 订单服务
 */
@Service
@Slf4j
public class OrderService {

    @Resource
    private OrderRepository orderRepository;

    @Resource
    private OrderItemRepository orderItemRepository;

    @Resource
    private UserServiceClient userServiceClient;

    @Resource
    private ProductServiceClient productServiceClient;

    @Resource
    private RedissonLockUtil redisLockUtil;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RabbitTemplate rabbitTemplate;
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.rabbitmq.template.exchange}")
    private String exchange;

    @Value("${spring.rabbitmq.template.routing-key}")
    private String routingKey;

    /**
     * 创建订单（分布式事务处理）
     */
    @GlobalTransactional
    public Result<OrderDTO> createOrder(String userId, String productId, Integer quantity) {
        // 生成订单号
        String orderNo = generateOrderNo();
        String lockKey = "create_order:" + userId + ":" + productId;
        RLock lock = null;

        try {
            // 获取分布式锁
            lock = redisLockUtil.tryLock(lockKey, 5, 2);
            if (lock == null) {
                return Result.fail(ErrorCode.LOCK_FAIL.getCode(), "创建订单过于频繁，请稍后再试");
            }

            // 1. 调用用户服务，获取用户信息
            Result<UserDTO> userResult = userServiceClient.getUserById(userId);
            if (userResult.getCode() != 200 || userResult.getData() == null) {
                return Result.fail("获取用户信息失败");
            }
            UserDTO user = userResult.getData();

            // 2. 调用产品服务，获取产品信息
            Result<ProductDTO> productResult = productServiceClient.getProductById(productId);
            if (productResult.getCode() != 200 || productResult.getData() == null) {
                return Result.fail("获取产品信息失败");
            }
            ProductDTO product = productResult.getData();

            // 3. 计算订单金额
            BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));

            // 4. 检查用户余额
            // 确保正确处理BigDecimal类型
            BigDecimal balance = user.getBalance();
            if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0 || balance.compareTo(totalAmount) < 0) {
                return Result.fail("用户余额不足");
            }

            // 5. 创建订单（预创建状态）
            Order order = new Order();
            order.setUserId(userId);
            order.setProductId(productId);
            order.setQuantity(quantity);
            order.setTotalAmount(totalAmount);
            order.setStatus(0); // 待支付
            order.setOrderNo(orderNo);
            order.setPaymentMethod("在线支付");

            boolean orderCreated = orderRepository.insert(order) > 0;
            if (!orderCreated) {
                return Result.fail("创建订单失败");
            }

            // 6. 创建订单项
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(productId);
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(product.getPrice());
            // 模拟设置图片URL
            String imageUrl = "https://example.com/product/" + product.getId() + ".jpg";
            orderItem.setImageUrl(imageUrl);
            orderItem.setTotalPrice(totalAmount);

            boolean itemCreated = orderItemRepository.insert(orderItem) > 0;
            if (!itemCreated) {
                throw new RuntimeException("创建订单项失败");
            }

            // 7. 调用用户服务扣减余额
            // 暂时使用double转换作为折中方案
            double amountToDeduct = totalAmount.doubleValue();
            // 注意：这里可能需要后续调整接口参数类型
            Result<Boolean> deductResult = userServiceClient.deductBalance(userId, (int)amountToDeduct);
            if (deductResult.getCode() != 200 || !deductResult.getData()) {
                throw new RuntimeException("扣减用户余额失败: " + deductResult.getMessage());
            }

            // 8. 调用产品服务扣减库存
            Result<Boolean> stockResult = productServiceClient.deductStock(productId, quantity);
            if (stockResult.getCode() != 200 || !stockResult.getData()) {
                throw new RuntimeException("扣减产品库存失败: " + stockResult.getMessage());
            }

            // 9. 更新订单状态为已支付
            order.setStatus(1);
            orderRepository.updateById(order);

            // 10. 发送订单创建成功消息
            sendOrderCreatedMessage(order.getId(), userId);

            // 构建返回结果
            OrderDTO orderDTO = new OrderDTO();
            BeanUtils.copyProperties(order, orderDTO);
            OrderItemDTO orderItemDTO = new OrderItemDTO();
            BeanUtils.copyProperties(orderItem, orderItemDTO);
            orderDTO.setOrderItems(List.of(orderItemDTO));

            return Result.success(orderDTO);
        } catch (Exception e) {
            log.error("创建订单失败: orderNo={}, userId={}, productId={}", orderNo, userId, productId, e);
            // 发送订单失败消息，用于后续的补偿机制
            sendOrderFailedMessage(userId, productId, quantity, orderNo, e.getMessage());
            return Result.fail("创建订单失败: " + e.getMessage());
        } finally {
            // 释放锁
            redisLockUtil.unlock(lock);
        }
    }

    /**
     * 获取所有订单列表
     */
    public Result<List<OrderDTO>> getAllOrders() {
        try {
            // 查询所有订单
            List<Order> orders = orderRepository.selectList(null);
            
            // 转换为DTO
            List<OrderDTO> orderDTOs = orders.stream().map(order -> {
                OrderDTO dto = new OrderDTO();
                BeanUtils.copyProperties(order, dto);
                
                // 查询每个订单的订单项
                List<OrderItem> items = orderItemRepository.selectByOrderId(order.getId());
                List<OrderItemDTO> itemDTOs = items.stream().map(item -> {
                    OrderItemDTO itemDTO = new OrderItemDTO();
                    BeanUtils.copyProperties(item, itemDTO);
                    return itemDTO;
                }).collect(Collectors.toList());
                
                dto.setOrderItems(itemDTOs);
                return dto;
            }).collect(Collectors.toList());
            
            return Result.success(orderDTOs);
        } catch (Exception e) {
            log.error("获取所有订单失败", e);
            return Result.fail("获取订单列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取订单详情
     */
    public Result<OrderDTO> getOrderById(String id) {
        // 先尝试从缓存获取 - 暂时注释掉Redis操作
        // String cacheKey = "order:info:" + id;
        // OrderDTO cachedOrder = (OrderDTO) redisTemplate.opsForValue().get(cacheKey);
        // if (cachedOrder != null) {
        //     return Result.success(cachedOrder);
        // }

        Order order = orderRepository.selectById(id);
        if (order == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }

        List<OrderItem> orderItems = orderItemRepository.selectByOrderId(id);
        List<OrderItemDTO> itemDTOs = orderItems.stream().map(item -> {
            OrderItemDTO dto = new OrderItemDTO();
            BeanUtils.copyProperties(item, dto);
            return dto;
        }).collect(Collectors.toList());

        OrderDTO orderDTO = new OrderDTO();
        BeanUtils.copyProperties(order, orderDTO);
        orderDTO.setOrderItems(itemDTOs);

        // 缓存订单信息，设置过期时间10分钟 - 暂时注释掉Redis操作
        // redisTemplate.opsForValue().set(cacheKey, orderDTO, 10, TimeUnit.MINUTES);

        return Result.success(orderDTO);
    }

    /**
     * 获取用户订单列表
     */
    public Result<List<OrderDTO>> getUserOrders(String userId) {
        List<Order> orders = orderRepository.selectByUserId(userId);
        List<OrderDTO> orderDTOs = orders.stream().map(order -> {
            OrderDTO dto = new OrderDTO();
            BeanUtils.copyProperties(order, dto);
            // 可以根据需要加载订单项
            return dto;
        }).collect(Collectors.toList());
        return Result.success(orderDTOs);
    }

    /**
     * 更新订单状态
     */
    @Transactional
    public Result<Boolean> updateOrderStatus(String id, Integer status) {
        Order order = orderRepository.selectById(id);
        if (order == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }

        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        boolean success = orderRepository.updateById(order) > 0;

        if (success) {
            // 清除缓存 - 暂时注释掉Redis操作
            // String cacheKey = "order:info:" + id;
            // redisTemplate.delete(cacheKey);
        }

        return success ? Result.success(true) : Result.fail("更新订单状态失败");
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return System.currentTimeMillis() + "" + uuid.substring(0, 10);
    }

    /**
     * 发送订单创建成功消息
     */
    private void sendOrderCreatedMessage(String orderId, String userId) {
        try {
            // 创建消息对象，包含orderId和userId
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("orderId", String.valueOf(orderId));
            orderData.put("userId", String.valueOf(userId));
            
            // 转换为JSON字符串
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonMessage = objectMapper.writeValueAsString(orderData);
            
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CREATE_ROUTING_KEY, jsonMessage);
            log.info("发送订单创建成功消息: orderId={}, userId={}", orderId, userId);
        } catch (Exception e) {
            log.error("发送订单创建成功消息失败", e);
        }
    }

    /**
     * 发送订单失败消息
     */
    private void sendOrderFailedMessage(String userId, String productId, Integer quantity, String orderNo, String errorMsg) {
        try {
            OrderFailedMessage message = new OrderFailedMessage();
            message.setUserId(String.valueOf(userId));
            message.setProductId(String.valueOf(productId));
            message.setQuantity(quantity);
            message.setOrderNo(orderNo);
            message.setErrorMsg(errorMsg);
            message.setCreateTime(LocalDateTime.now());
            
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.STOCK_DEDUCT_FAIL_ROUTING_KEY, message);
            log.info("发送订单失败消息: {}", message);
        } catch (Exception e) {
            log.error("发送订单失败消息失败", e);
        }
    }

    /**
     * 订单失败消息内部类
     */
    private static class OrderFailedMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        private String userId;
        private String productId;
        private Integer quantity;
        private String orderNo;
        private String errorMsg;
        private LocalDateTime createTime;

        // getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getErrorMsg() { return errorMsg; }
        public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

        @Override
        public String toString() {
            return "OrderFailedMessage{" +
                    "userId=" + userId +
                    ", productId=" + productId +
                    ", quantity=" + quantity +
                    ", orderNo='" + orderNo + '\'' +
                    ", errorMsg='" + errorMsg + '\'' +
                    ", createTime=" + createTime +
                    '}';
        }
    }
}