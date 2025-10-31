import React, { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, Typography, message } from 'antd';
import api from '../utils/axiosConfig';

const { Title } = Typography;
const { TextArea } = Input;

const Products = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [visible, setVisible] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [form] = Form.useForm();
  const [adjustForm] = Form.useForm(); // 为库存调整创建表单实例

  // 获取产品列表
  const fetchProducts = async () => {
    setLoading(true);
    try {
      // 使用全局axios配置，自动处理大整数ID为字符串
      const response = await api.get('/products');
      if (response.data.code === 200) {
        // 额外确保所有产品ID都是字符串，提供双重保障
        const processedProducts = response.data.data.map(product => ({
          ...product,
          id: String(product.id)
        }));
        setProducts(processedProducts);
      } else {
        message.error(response.data.message || '获取产品列表失败');
      }
    } catch (error) {
      console.error('获取产品列表失败:', error);
      message.error('获取产品列表失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, []);

  // 打开新增/编辑对话框
  const showModal = (product = null) => {
    if (product) {
      // 创建副本并确保ID为字符串
      const productCopy = { ...product, id: String(product.id) };
      setEditingProduct(productCopy);
      form.setFieldsValue({
        name: product.name,
        description: product.description,
        price: product.price,
        stock: product.stock
      });
    } else {
      setEditingProduct(null);
      form.resetFields();
    }
    setVisible(true);
  };

  // 关闭对话框
  const handleCancel = () => {
    setVisible(false);
    setEditingProduct(null);
    form.resetFields();
  };

  // 保存产品
  const handleSave = async (values) => {
    try {
      // 转换价格和库存为正确的数字类型
      const productData = {
        ...values,
        price: Number(values.price),
        stock: Number(values.stock)
      };
      
      let response;
      if (editingProduct) {
        // 更新产品 - 确保ID作为字符串传递
        response = await api.put(`/products/${String(editingProduct.id)}`, productData);
      } else {
        // 新增产品
        response = await api.post('/products', productData);
      }
      
      if (response.data.code === 200) {
        message.success(editingProduct ? '更新产品成功' : '新增产品成功');
        setVisible(false);
        fetchProducts();
      } else {
        message.error(response.data.message || (editingProduct ? '更新产品失败' : '新增产品失败'));
      }
    } catch (error) {
      console.error('保存产品失败:', error);
      message.error('保存产品失败，请稍后重试');
    }
  };

  // 删除产品
  const handleDelete = async (id) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个产品吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
            // 确保传递的是字符串类型的ID
            const response = await api.delete(`/products/${String(id)}`);
          if (response.data.code === 200) {
            message.success('删除产品成功');
            fetchProducts();
          } else {
            message.error(response.data.message || '删除产品失败');
          }
        } catch (error) {
          console.error('删除产品失败:', error);
          message.error('删除产品失败，请稍后重试');
        }
      }
    });
  };

  // 调整库存 - 使用React状态管理代替DOM操作
  const handleAdjustStock = async (product) => {
    // 重置调整表单
    adjustForm.resetFields();
    
    Modal.confirm({
      title: '调整库存',
      content: (
        <Form form={adjustForm} layout="vertical">
          <Form.Item label="当前库存">
            <Input value={product.stock} disabled />
          </Form.Item>
          <Form.Item 
            label="调整数量" 
            name="adjustAmount"
            rules={[
              {
                validator: (_, value) => {
                  const numValue = Number(value);
                  if (isNaN(numValue) || numValue === 0) {
                    return Promise.reject('请输入非零的调整数量');
                  }
                  return Promise.resolve();
                }
              }
            ]}
          >
            <Input 
              type="number" 
              placeholder="正数增加，负数减少" 
              autoFocus
            />
          </Form.Item>
        </Form>
      ),
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          // 获取并验证表单值
          const values = await adjustForm.validateFields();
          const adjustAmount = Number(values.adjustAmount);
          
          // 确保传递的是字符串类型的ID
          const response = await api.put(
            `/products/${String(product.id)}/stock`,
            null,
            { params: { amount: adjustAmount } }
          );
          if (response.data.code === 200) {
            message.success('库存调整成功');
            fetchProducts();
          } else {
            message.error(response.data.message || '库存调整失败');
          }
        } catch (error) {
          // 表单验证失败时不显示错误消息
          if (error.message !== 'Validate Failed') {
            console.error('调整库存失败:', error);
            message.error('调整库存失败，请稍后重试');
          }
        }
      }
    });
  };

  const columns = [
    {
      title: '产品ID',
      dataIndex: 'id',
      key: 'id',
      // 确保ID作为字符串显示，避免JavaScript精度问题
      render: id => String(id),
    },
    {
      title: '产品名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: '价格',
      dataIndex: 'price',
      key: 'price',
      render: price => `¥${price.toFixed(2)}`,
    },
    {
      title: '库存',
      dataIndex: 'stock',
      key: 'stock',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <>
          <Button type="link" onClick={() => showModal(record)}>编辑</Button>
          <Button type="link" onClick={() => handleAdjustStock(record)}>调整库存</Button>
          <Button type="link" danger onClick={() => handleDelete(String(record.id))}>删除</Button>
        </>
      ),
    },
  ];

  return (
    <div>
      <Title level={3}>产品管理</Title>
      <Button type="primary" onClick={() => showModal()} style={{ marginBottom: 16 }}>
        新增产品
      </Button>
      <Table columns={columns} dataSource={products} rowKey={(record) => String(record.id)} loading={loading} />
      
      <Modal
        title={editingProduct ? '编辑产品' : '新增产品'}
        open={visible}
        onOk={form.submit}
        onCancel={handleCancel}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSave}
        >
          <Form.Item
            label="产品名称"
            name="name"
            rules={[{ required: true, message: '请输入产品名称' }]}
          >
            <Input placeholder="请输入产品名称" />
          </Form.Item>
          
          <Form.Item
            label="描述"
            name="description"
          >
            <TextArea rows={4} placeholder="请输入产品描述" />
          </Form.Item>
          
          <Form.Item
            label="价格"
            name="price"
            rules={[
              { required: true, message: '请输入价格' },
              {
                validator: (_, value) => {
                  const numValue = Number(value);
                  if (isNaN(numValue) || numValue < 0) {
                    return Promise.reject('请输入有效的价格（大于等于0的数字）');
                  }
                  return Promise.resolve();
                }
              }
            ]}
          >
            <Input type="number" placeholder="请输入价格" />
          </Form.Item>
          
          <Form.Item
            label="库存"
            name="stock"
            rules={[
              { required: true, message: '请输入库存' },
              {
                validator: (_, value) => {
                  const numValue = Number(value);
                  if (isNaN(numValue) || numValue < 0 || !Number.isInteger(numValue)) {
                    return Promise.reject('请输入有效的库存（大于等于0的整数）');
                  }
                  return Promise.resolve();
                }
              }
            ]}
          >
            <Input type="number" placeholder="请输入库存" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Products;