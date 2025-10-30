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
import com.example.common.util.RedisLockUtil;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.OrderItemRepository;
import com.example.productservice.dto.ProductDTO;
import com.example.userservice.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private RedisLockUtil redisLockUtil;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.template.exchange}")
    private String exchange;

    @Value("${spring.rabbitmq.template.routing-key}")
    private String routingKey;

    /**
     * 创建订单（分布式事务处理）
     */
    @Transactional
    public Result<OrderDTO> createOrder(Long userId, Long productId, Integer quantity) {
        // 生成订单号
        String orderNo = generateOrderNo();
        String lockKey = "create_order:" + userId + ":" + productId;
        String lockValue = null;

        try {
            // 获取分布式锁
            lockValue = redisLockUtil.tryLock(lockKey, 5, 2);
            if (lockValue == null) {
                return Result.fail(ErrorCode.LOCK_FAIL.getCode(), "创建订单过于频繁，请稍后再试");
            }

            // 1. 调用用户服务，获取用户信息
            Result<UserDTO> userResult = userServiceClient.getUserById(userId);
            if (!userResult.getCode().equals(200) || userResult.getData() == null) {
                return Result.fail("获取用户信息失败");
            }
            UserDTO user = userResult.getData();

            // 2. 调用产品服务，获取产品信息
            Result<ProductDTO> productResult = productServiceClient.getProductById(productId);
            if (!productResult.getCode().equals(200) || productResult.getData() == null) {
                return Result.fail("获取产品信息失败");
            }
            ProductDTO product = productResult.getData();

            // 3. 计算订单金额
            BigDecimal totalAmount = product.getPrice().multiply(new BigDecimal(quantity));

            // 4. 检查用户余额
            if (user.getBalance() < totalAmount.intValue()) {
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
            orderItem.setUnitPrice(new BigDecimal(product.getPrice()));
            orderItem.setTotalPrice(totalAmount);
            orderItem.setImageUrl(product.getImageUrl());

            boolean itemCreated = orderItemRepository.insert(orderItem) > 0;
            if (!itemCreated) {
                throw new RuntimeException("创建订单项失败");
            }

            // 7. 调用用户服务扣减余额
            Result<Boolean> deductResult = userServiceClient.deductBalance(userId, totalAmount.intValue());
            if (!deductResult.getCode().equals(200) || !deductResult.getData()) {
                throw new RuntimeException("扣减用户余额失败: " + deductResult.getMessage());
            }

            // 8. 调用产品服务扣减库存
            Result<Boolean> stockResult = productServiceClient.deductStock(productId, quantity);
            if (!stockResult.getCode().equals(200) || !stockResult.getData()) {
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
            if (lockValue != null) {
                redisLockUtil.unlock(lockKey, lockValue);
            }
        }
    }

    /**
     * 获取订单详情
     */
    public Result<OrderDTO> getOrderById(Long id) {
        // 先尝试从缓存获取
        String cacheKey = "order:info:" + id;
        OrderDTO cachedOrder = (OrderDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedOrder != null) {
            return Result.success(cachedOrder);
        }

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

        // 缓存订单信息，设置过期时间10分钟
        redisTemplate.opsForValue().set(cacheKey, orderDTO, 10, TimeUnit.MINUTES);

        return Result.success(orderDTO);
    }

    /**
     * 获取用户订单列表
     */
    public Result<List<OrderDTO>> getUserOrders(Long userId) {
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
    public Result<Boolean> updateOrderStatus(Long id, Integer status) {
        Order order = orderRepository.selectById(id);
        if (order == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }

        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        boolean success = orderRepository.updateById(order) > 0;

        if (success) {
            // 清除缓存
            String cacheKey = "order:info:" + id;
            redisTemplate.delete(cacheKey);
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
    private void sendOrderCreatedMessage(Long orderId, Long userId) {
        try {
            // 创建消息对象，包含orderId和userId
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("orderId", orderId);
            orderData.put("userId", userId);
            
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
    private void sendOrderFailedMessage(Long userId, Long productId, Integer quantity, String orderNo, String errorMsg) {
        try {
            OrderFailedMessage message = new OrderFailedMessage();
            message.setUserId(userId);
            message.setProductId(productId);
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
    private static class OrderFailedMessage {
        private Long userId;
        private Long productId;
        private Integer quantity;
        private String orderNo;
        private String errorMsg;
        private LocalDateTime createTime;

        // getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
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