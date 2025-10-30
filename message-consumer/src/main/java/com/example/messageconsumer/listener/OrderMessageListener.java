package com.example.messageconsumer.listener;

import com.example.messageconsumer.feign.OrderServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 订单消息监听器
 */
@Component
@Slf4j
public class OrderMessageListener {

    @Autowired
    private OrderServiceClient orderServiceClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // 订单状态常量
    private static final Integer ORDER_STATUS_PENDING = 1;  // 待支付
    private static final Integer ORDER_STATUS_PAID = 2;      // 已支付
    private static final Integer ORDER_STATUS_CANCELLED = 3; // 已取消

    /**
     * 监听订单创建消息
     */
    @RabbitListener(queues = "order.create.queue", ackMode = "MANUAL")
    public void handleOrderCreate(Message message, org.springframework.amqp.core.Channel channel) throws Exception {
        try {
            String msg = new String(message.getBody());
            Map<String, Object> orderData = objectMapper.readValue(msg, Map.class);
            Long orderId = ((Number) orderData.get("orderId")).longValue();
            Long userId = ((Number) orderData.get("userId")).longValue();
            log.info("收到订单创建消息: orderId={}, userId={}", orderId, userId);

            // 1. 记录订单创建日志（实际项目中可以写入数据库或日志系统）
            // 2. 启动订单超时检查（这里使用Redis过期键实现）
            String orderTimeoutKey = "order:timeout:" + orderId;
            redisTemplate.opsForValue().set(orderTimeoutKey, orderId, 30, TimeUnit.MINUTES);
            redisTemplate.expire(orderTimeoutKey, 30, TimeUnit.MINUTES);

            // 3. 发送订单创建成功的通知（如邮件、短信等，实际项目中实现）
            // TODO: 实现通知逻辑

            log.info("订单创建消息处理完成: orderId={}", orderId);
            // 确认消息已处理
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理订单创建消息失败", e);
            // 拒绝消息并重新入队
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    /**
     * 监听订单支付消息
     */
    @RabbitListener(queues = "order.pay.queue", ackMode = "MANUAL")
    public void handleOrderPay(Message message, org.springframework.amqp.core.Channel channel) throws Exception {
        try {
            String msg = new String(message.getBody());
            Map<String, Object> orderData = objectMapper.readValue(msg, Map.class);
            Long orderId = ((Number) orderData.get("orderId")).longValue();
            String transactionId = (String) orderData.get("transactionId");
            log.info("收到订单支付消息: orderId={}, transactionId={}", orderId, transactionId);

            // 1. 更新订单状态为已支付
            com.example.common.entity.Result<Boolean> result = orderServiceClient.updateOrderStatus(orderId, ORDER_STATUS_PAID);
            if (result.isSuccess()) {
                // 2. 取消订单超时检查
                String orderTimeoutKey = "order:timeout:" + orderId;
                redisTemplate.delete(orderTimeoutKey);

                // 3. 发送订单支付成功的通知
                // TODO: 实现通知逻辑

                log.info("订单支付成功: orderId={}", orderId);
            } else {
                log.error("更新订单状态失败: orderId={}, message={}", orderId, result.getMessage());
            }

            // 确认消息已处理
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理订单支付消息失败", e);
            // 拒绝消息并重新入队
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    /**
     * 监听订单超时消息
     */
    @RabbitListener(queues = "order.timeout.queue", ackMode = "MANUAL")
    public void handleOrderTimeout(Message message, org.springframework.amqp.core.Channel channel) throws Exception {
        try {
            String msg = new String(message.getBody());
            Map<String, Object> orderData = objectMapper.readValue(msg, Map.class);
            Long orderId = ((Number) orderData.get("orderId")).longValue();
            log.info("收到订单超时消息: orderId={}", orderId);

            // 1. 更新订单状态为已取消
            com.example.common.entity.Result<Boolean> result = orderServiceClient.updateOrderStatus(orderId, ORDER_STATUS_CANCELLED);
            if (result.isSuccess()) {
                // 2. 发送库存回滚消息
                Map<String, Object> stockRollbackData = Map.of(
                        "orderId", orderId,
                        "action", "rollback"
                );
                rabbitTemplate.convertAndSend("stock.exchange", "stock.rollback", objectMapper.writeValueAsString(stockRollbackData));

                // 3. 发送订单取消通知
                // TODO: 实现通知逻辑

                log.info("订单超时处理完成: orderId={}", orderId);
            } else {
                log.error("更新订单状态失败: orderId={}, message={}", orderId, result.getMessage());
            }

            // 确认消息已处理
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理订单超时消息失败", e);
            // 拒绝消息并重新入队
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    /**
     * 监听库存扣减失败消息
     */
    @RabbitListener(queues = "stock.deduct.fail.queue", ackMode = "MANUAL")
    public void handleStockDeductFail(Message message, org.springframework.amqp.core.Channel channel) throws Exception {
        try {
            String msg = new String(message.getBody());
            Map<String, Object> stockData = objectMapper.readValue(msg, Map.class);
            Long orderId = ((Number) stockData.get("orderId")).longValue();
            String errorMsg = (String) stockData.get("errorMsg");
            log.warn("收到库存扣减失败消息: orderId={}, errorMsg={}", orderId, errorMsg);

            // 1. 更新订单状态为已取消
            orderServiceClient.updateOrderStatus(orderId, ORDER_STATUS_CANCELLED);

            // 2. 发送订单创建失败通知
            // TODO: 实现通知逻辑

            log.info("库存扣减失败处理完成: orderId={}", orderId);
            // 确认消息已处理
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理库存扣减失败消息失败", e);
            // 拒绝消息并重新入队
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}