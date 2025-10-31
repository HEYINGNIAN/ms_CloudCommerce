-- 用户表 - 修改ID字段为VARCHAR类型
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` VARCHAR(32) NOT NULL COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `nickname` VARCHAR(50) NOT NULL COMMENT '昵称',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `role` INT DEFAULT 2 COMMENT '角色(1:管理员,2:普通用户)',
  `balance` DECIMAL(10,2) DEFAULT 0.00 COMMENT '余额',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 产品表 - 修改ID字段为VARCHAR类型
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id` VARCHAR(32) NOT NULL COMMENT '产品ID',
  `name` VARCHAR(100) NOT NULL COMMENT '产品名称',
  `description` TEXT COMMENT '产品描述',
  `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
  `stock` INT NOT NULL COMMENT '库存',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

-- 订单表 - 修改ID字段和关联字段为VARCHAR类型
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
  `id` VARCHAR(32) NOT NULL COMMENT '订单ID',
  `user_id` VARCHAR(32) NOT NULL COMMENT '用户ID',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `status` INT DEFAULT 1 COMMENT '订单状态(1:待支付,2:已支付,3:已取消)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单项表 - 修改ID字段和关联字段为VARCHAR类型
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id` VARCHAR(32) NOT NULL COMMENT '订单项ID',
  `order_id` VARCHAR(32) NOT NULL COMMENT '订单ID',
  `product_id` VARCHAR(32) NOT NULL COMMENT '产品ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '产品名称',
  `price` DECIMAL(10,2) NOT NULL COMMENT '产品单价',
  `quantity` INT NOT NULL COMMENT '购买数量',
  `subtotal` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

-- 初始化一些测试数据
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `role`, `balance`) VALUES
('1', 'admin', '$2a$10$e9V3xJgWx8eWlQ9XKd8Z6eV7aF5h5b5b8c5d5e5f5g5h5i5j5k5l5m5n5o', '管理员', 'admin@example.com', '13800138000', 1, 10000.00),
('2', 'user1', '$2a$10$e9V3xJgWx8eWlQ9XKd8Z6eV7aF5h5b5b8c5d5e5f5g5h5i5j5k5l5m5n5o', '测试用户1', 'user1@example.com', '13800138001', 2, 5000.00);

INSERT INTO `product` (`id`, `name`, `description`, `price`, `stock`) VALUES
('1', '测试产品1', '这是第一个测试产品', 99.99, 100),
('2', '测试产品2', '这是第二个测试产品', 199.99, 50);