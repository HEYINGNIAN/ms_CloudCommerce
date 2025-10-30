import React, { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, Typography, message } from 'antd';
import axios from 'axios';

const { Title } = Typography;
const { TextArea } = Input;

const Products = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [visible, setVisible] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [form] = Form.useForm();

  // 获取产品列表
  const fetchProducts = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/api/products');
      if (response.data.code === 200) {
        setProducts(response.data.data);
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
      setEditingProduct(product);
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
      let response;
      if (editingProduct) {
        // 更新产品
        response = await axios.put(`/api/products/${editingProduct.id}`, values);
      } else {
        // 新增产品
        response = await axios.post('/api/products', values);
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
          const response = await axios.delete(`/api/products/${id}`);
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

  // 调整库存
  const handleAdjustStock = async (product) => {
    Modal.confirm({
      title: '调整库存',
      content: (
        <Form layout="vertical">
          <Form.Item label="当前库存">
            <Input value={product.stock} disabled />
          </Form.Item>
          <Form.Item label="调整数量">
            <Input 
              id="adjustAmount" 
              type="number" 
              placeholder="正数增加，负数减少" 
              focus={true}
            />
          </Form.Item>
        </Form>
      ),
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const adjustAmount = parseInt(document.getElementById('adjustAmount').value) || 0;
        if (adjustAmount === 0) {
          message.warning('请输入调整数量');
          return;
        }
        
        try {
          const response = await axios.put(
            `/api/products/${product.id}/stock`,
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
          console.error('调整库存失败:', error);
          message.error('调整库存失败，请稍后重试');
        }
      }
    });
  };

  const columns = [
    {
      title: '产品ID',
      dataIndex: 'id',
      key: 'id',
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
          <Button type="link" danger onClick={() => handleDelete(record.id)}>删除</Button>
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
      <Table columns={columns} dataSource={products} rowKey="id" loading={loading} />
      
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
            rules={[{ required: true, message: '请输入价格' }, { type: 'number', min: 0 }]}
          >
            <Input type="number" placeholder="请输入价格" />
          </Form.Item>
          
          <Form.Item
            label="库存"
            name="stock"
            rules={[{ required: true, message: '请输入库存' }, { type: 'number', min: 0 }]}
          >
            <Input type="number" placeholder="请输入库存" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Products;