import React, { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Select, Input, Typography, message } from 'antd';
import api from '../utils/axiosConfig';

const { Title } = Typography;
const { Option } = Select;

const Orders = () => {
  const [orders, setOrders] = useState([]);
  const [products, setProducts] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [createVisible, setCreateVisible] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [form] = Form.useForm();

  // 获取订单列表
  const fetchOrders = async () => {
    setLoading(true);
    try {
      // 使用全局axios配置，自动处理大整数ID为字符串
      const response = await api.get('/orders');
      if (response.data.code === 200) {
        setOrders(response.data.data);
      } else {
        message.error(response.data.message || '获取订单列表失败');
      }
    } catch (error) {
      console.error('获取订单列表失败:', error);
      message.error('获取订单列表失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  // 获取产品列表
  const fetchProducts = async () => {
    try {
      // 使用全局axios配置，自动处理大整数ID为字符串
      const response = await api.get('/products');
      if (response.data.code === 200) {
        // JSONBig已经在axios配置中处理了大整数精度问题
        setProducts(response.data.data);
      }
    } catch (error) {
      console.error('获取产品列表失败:', error);
    }
  };

  // 获取用户列表
  const fetchUsers = async () => {
    try {
      // 使用全局axios配置，自动处理大整数ID为字符串
      const response = await api.get('/users');
      if (response.data.code === 200) {
        // JSONBig已经在axios配置中处理了大整数精度问题
        setUsers(response.data.data);
      }
    } catch (error) {
      console.error('获取用户列表失败:', error);
    }
  };

  useEffect(() => {
    fetchOrders();
    fetchProducts();
    fetchUsers();
  }, []);

  // 打开创建订单对话框
  const showCreateModal = () => {
    form.resetFields();
    setCreateVisible(true);
  };

  // 关闭对话框
  const handleCancel = () => {
    setCreateVisible(false);
    setDetailVisible(false);
    setSelectedOrder(null);
    form.resetFields();
  };

  // 创建订单
  const handleCreateOrder = async (values) => {
    try {
      // 注意：ID作为字符串处理以避免JavaScript长整数精度丢失问题
      // 数量仍转换为数字类型
      const orderData = {
        userId: values.userId, // 保持字符串类型
        productId: values.productId, // 保持字符串类型
        quantity: Number(values.quantity)
      };
      
      const response = await api.post('/orders', null, {
        params: orderData
      });
      
      if (response.data.code === 200) {
        message.success('创建订单成功');
        setCreateVisible(false);
        fetchOrders();
      } else {
        message.error(response.data.message || '创建订单失败');
      }
    } catch (error) {
      console.error('创建订单失败:', error);
      message.error('创建订单失败，请稍后重试');
    }
  };

  // 查看订单详情
  const handleViewDetail = async (id) => {
    try {
      // 确保传递的是字符串类型的订单ID
      const response = await api.get(`/orders/${String(id)}`);
      if (response.data.code === 200) {
        setSelectedOrder(response.data.data);
        setDetailVisible(true);
      } else {
        message.error(response.data.message || '获取订单详情失败');
      }
    } catch (error) {
      console.error('获取订单详情失败:', error);
      message.error('获取订单详情失败，请稍后重试');
    }
  };

  // 支付订单
  const handlePayOrder = async (orderId) => {
    // 确保orderId是字符串类型
    const stringOrderId = String(orderId);
    Modal.confirm({
      title: '确认支付',
      content: '确定要支付这个订单吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          // 模拟支付接口调用
          // 实际项目中应该调用支付服务
          message.success('支付成功');
          // 发送订单支付消息
          const transactionId = 'TXN' + Date.now();
          await api.post('/orders/pay', null, {
            params: { orderId: stringOrderId, transactionId }
          });
          fetchOrders();
        } catch (error) {
          console.error('支付失败:', error);
          message.error('支付失败，请稍后重试');
        }
      }
    });
  };

  const getOrderStatusText = (status) => {
    switch (status) {
      case 1: return '待支付';
      case 2: return '已支付';
      case 3: return '已取消';
      default: return '未知状态';
    }
  };

  const getOrderStatusColor = (status) => {
    switch (status) {
      case 1: return 'orange';
      case 2: return 'green';
      case 3: return 'red';
      default: return 'default';
    }
  };

  const columns = [
    {
      title: '订单ID',
      dataIndex: 'id',
      key: 'id',
      // 确保订单ID作为字符串显示
      render: id => String(id)
    },
    {      title: '用户ID',
      dataIndex: 'userId',
      key: 'userId',
      render: userId => {
        // 确保比较时使用字符串类型，避免类型不匹配
        const user = users.find(u => String(u.id) === String(userId));
        return user ? user.username : String(userId);
      }
    },
    {
      title: '订单金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      render: amount => `¥${(amount || 0).toFixed(2)}`,
    },
    {
      title: '订单状态',
      dataIndex: 'status',
      key: 'status',
      render: status => (
        <span style={{ color: getOrderStatusColor(status) }}>
          {getOrderStatusText(status)}
        </span>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <>
          <Button type="link" onClick={() => handleViewDetail(String(record.id))}>详情</Button>
          {record.status === 1 && (
            <Button type="link" onClick={() => handlePayOrder(String(record.id))}>支付</Button>
          )}
        </>
      ),
    },
  ];

  return (
    <div>
      <Title level={3}>订单管理</Title>
      <Button type="primary" onClick={showCreateModal} style={{ marginBottom: 16 }}>
        创建订单
      </Button>
      <Table columns={columns} dataSource={orders} rowKey={(record) => String(record.id)} loading={loading} />
      
      {/* 创建订单对话框 */}
      <Modal
        title="创建订单"
        open={createVisible}
        onOk={form.submit}
        onCancel={handleCancel}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreateOrder}
        >
          <Form.Item
            label="用户"
            name="userId"
            rules={[{ required: true, message: '请选择用户' }]}
          >
            <Select placeholder="请选择用户">
              {users.map(user => (
                <Option key={String(user.id)} value={String(user.id)}>{user.username} (¥{user.balance})</Option>
              ))}
            </Select>
          </Form.Item>
          
          <Form.Item
            label="产品"
            name="productId"
            rules={[{ required: true, message: '请选择产品' }]}
          >
            <Select placeholder="请选择产品">
              {products.map(product => (
                <Option key={String(product.id)} value={String(product.id)}>
                  {product.name} - ¥{product.price.toFixed(2)} (库存: {product.stock})
                </Option>
              ))}
            </Select>
          </Form.Item>
          
          <Form.Item
            label="数量"
            name="quantity"
            rules={[
              { required: true, message: '请输入数量' },
              {
                validator: (_, value) => {
                  const numValue = Number(value);
                  if (isNaN(numValue) || numValue < 1 || !Number.isInteger(numValue)) {
                    return Promise.reject('请输入有效的数量（大于等于1的整数）');
                  }
                  return Promise.resolve();
                }
              }
            ]}
          >
            <Input type="number" placeholder="请输入购买数量" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 订单详情对话框 */}
      <Modal
        title="订单详情"
        open={detailVisible}
        onCancel={handleCancel}
        footer={null}
        width={800}
      >
        {selectedOrder && (
          <div>
            <div style={{ marginBottom: 20 }}>
              <h3>订单基本信息</h3>
              <p>订单ID: {String(selectedOrder.id)}</p>
              <p>用户ID: {String(selectedOrder.userId)}</p>
              <p>订单金额: ¥{(selectedOrder.totalAmount || 0).toFixed(2)}</p>
              <p>订单状态: 
                <span style={{ color: getOrderStatusColor(selectedOrder.status), marginLeft: 5 }}>
                  {getOrderStatusText(selectedOrder.status)}
                </span>
              </p>
              <p>创建时间: {selectedOrder.createTime}</p>
              {selectedOrder.updateTime && (
                <p>更新时间: {selectedOrder.updateTime}</p>
              )}
            </div>
            
            {selectedOrder.orderItems && selectedOrder.orderItems.length > 0 && (
              <div>
                <h3>订单商品</h3>
                <Table
                  columns={[
                    { title: '商品ID', dataIndex: 'productId', key: 'productId', render: id => String(id) },
                    { title: '商品名称', dataIndex: 'productName', key: 'productName' },
                    { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', render: price => `¥${price ? price.toFixed(2) : '0.00'}` },
                    { title: '数量', dataIndex: 'quantity', key: 'quantity' },
                    { title: '小计', dataIndex: 'totalPrice', key: 'totalPrice', render: subtotal => `¥${subtotal ? subtotal.toFixed(2) : '0.00'}` }
                  ]}
                  dataSource={selectedOrder.orderItems}
                  rowKey={(record) => String(record.id)}
                  pagination={false}
                />
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
};

export default Orders;