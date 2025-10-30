package com.example.orderservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 */
@Configuration
public class RabbitMQConfig {

    // 订单交换机
    public static final String ORDER_EXCHANGE = "order.exchange";
    
    // 订单创建队列
    public static final String ORDER_CREATE_QUEUE = "order.create.queue";
    
    // 订单支付队列
    public static final String ORDER_PAY_QUEUE = "order.pay.queue";
    
    // 订单超时队列
    public static final String ORDER_TIMEOUT_QUEUE = "order.timeout.queue";
    
    // 库存扣减失败队列
    public static final String STOCK_DEDUCT_FAIL_QUEUE = "stock.deduct.fail.queue";
    
    // 订单创建路由键
    public static final String ORDER_CREATE_ROUTING_KEY = "order.create";
    
    // 订单支付路由键
    public static final String ORDER_PAY_ROUTING_KEY = "order.pay";
    
    // 订单超时路由键
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout";
    
    // 库存扣减失败路由键
    public static final String STOCK_DEDUCT_FAIL_ROUTING_KEY = "stock.deduct.fail";

    /**
     * 创建订单交换机
     */
    @Bean
    public Exchange orderExchange() {
        return ExchangeBuilder.topicExchange(ORDER_EXCHANGE).durable(true).build();
    }

    /**
     * 创建订单创建队列
     */
    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder.durable(ORDER_CREATE_QUEUE).build();
    }

    /**
     * 创建订单支付队列
     */
    @Bean
    public Queue orderPayQueue() {
        return QueueBuilder.durable(ORDER_PAY_QUEUE).build();
    }

    /**
     * 创建订单超时队列
     */
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE).build();
    }

    /**
     * 创建库存扣减失败队列
     */
    @Bean
    public Queue stockDeductFailQueue() {
        return QueueBuilder.durable(STOCK_DEDUCT_FAIL_QUEUE).build();
    }

    /**
     * 绑定订单创建队列到交换机
     */
    @Bean
    public Binding bindingOrderCreateQueue(Queue orderCreateQueue, Exchange orderExchange) {
        return BindingBuilder.bind(orderCreateQueue).to(orderExchange).with(ORDER_CREATE_ROUTING_KEY).noargs();
    }

    /**
     * 绑定订单支付队列到交换机
     */
    @Bean
    public Binding bindingOrderPayQueue(Queue orderPayQueue, Exchange orderExchange) {
        return BindingBuilder.bind(orderPayQueue).to(orderExchange).with(ORDER_PAY_ROUTING_KEY).noargs();
    }

    /**
     * 绑定订单超时队列到交换机
     */
    @Bean
    public Binding bindingOrderTimeoutQueue(Queue orderTimeoutQueue, Exchange orderExchange) {
        return BindingBuilder.bind(orderTimeoutQueue).to(orderExchange).with(ORDER_TIMEOUT_ROUTING_KEY).noargs();
    }

    /**
     * 绑定库存扣减失败队列到交换机
     */
    @Bean
    public Binding bindingStockDeductFailQueue(Queue stockDeductFailQueue, Exchange orderExchange) {
        return BindingBuilder.bind(stockDeductFailQueue).to(orderExchange).with(STOCK_DEDUCT_FAIL_ROUTING_KEY).noargs();
    }
}