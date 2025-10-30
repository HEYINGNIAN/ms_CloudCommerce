import React, { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, Select, Typography, message } from 'antd';
import axios from 'axios';

const { Title } = Typography;
const { Option } = Select;
const { TextArea } = Input;

const Users = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [visible, setVisible] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [form] = Form.useForm();

  // 获取用户列表
  const fetchUsers = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/api/users');
      if (response.data.code === 200) {
        setUsers(response.data.data);
      } else {
        message.error(response.data.message || '获取用户列表失败');
      }
    } catch (error) {
      console.error('获取用户列表失败:', error);
      message.error('获取用户列表失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  // 打开新增/编辑对话框
  const showModal = (user = null) => {
    if (user) {
      setEditingUser(user);
      form.setFieldsValue({
        username: user.username,
        password: '',
        nickname: user.nickname,
        email: user.email,
        phone: user.phone,
        role: user.role || 2
      });
    } else {
      setEditingUser(null);
      form.resetFields();
    }
    setVisible(true);
  };

  // 关闭对话框
  const handleCancel = () => {
    setVisible(false);
    setEditingUser(null);
    form.resetFields();
  };

  // 保存用户
  const handleSave = async (values) => {
    try {
      let response;
      if (editingUser) {
        // 更新用户
        response = await axios.put(`/api/users/${editingUser.id}`, values);
      } else {
        // 新增用户
        response = await axios.post('/api/users', values);
      }
      
      if (response.data.code === 200) {
        message.success(editingUser ? '更新用户成功' : '新增用户成功');
        setVisible(false);
        fetchUsers();
      } else {
        message.error(response.data.message || (editingUser ? '更新用户失败' : '新增用户失败'));
      }
    } catch (error) {
      console.error('保存用户失败:', error);
      message.error('保存用户失败，请稍后重试');
    }
  };

  // 删除用户
  const handleDelete = async (id) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个用户吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          const response = await axios.delete(`/api/users/${id}`);
          if (response.data.code === 200) {
            message.success('删除用户成功');
            fetchUsers();
          } else {
            message.error(response.data.message || '删除用户失败');
          }
        } catch (error) {
          console.error('删除用户失败:', error);
          message.error('删除用户失败，请稍后重试');
        }
      }
    });
  };

  const columns = [
    {
      title: '用户ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: '昵称',
      dataIndex: 'nickname',
      key: 'nickname',
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: role => role === 1 ? '管理员' : '普通用户',
    },
    {
      title: '余额',
      dataIndex: 'balance',
      key: 'balance',
      render: balance => `¥${balance || 0}`,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <>
          <Button type="link" onClick={() => showModal(record)}>编辑</Button>
          <Button type="link" danger onClick={() => handleDelete(record.id)}>删除</Button>
        </>
      ),
    },
  ];

  return (
    <div>
      <Title level={3}>用户管理</Title>
      <Button type="primary" onClick={() => showModal()} style={{ marginBottom: 16 }}>
        新增用户
      </Button>
      <Table columns={columns} dataSource={users} rowKey="id" loading={loading} />
      
      <Modal
        title={editingUser ? '编辑用户' : '新增用户'}
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
            label="用户名"
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="请输入用户名" disabled={!!editingUser} />
          </Form.Item>
          
          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: !editingUser, message: '请输入密码' }]}
          >
            <Input.Password placeholder={editingUser ? '不修改密码请留空' : '请输入密码'} />
          </Form.Item>
          
          <Form.Item
            label="昵称"
            name="nickname"
            rules={[{ required: true, message: '请输入昵称' }]}
          >
            <Input placeholder="请输入昵称" />
          </Form.Item>
          
          <Form.Item
            label="邮箱"
            name="email"
            rules={[{ required: true, type: 'email', message: '请输入有效的邮箱地址' }]}
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>
          
          <Form.Item
            label="手机号"
            name="phone"
            rules={[{ required: true, message: '请输入手机号' }]}
          >
            <Input placeholder="请输入手机号" />
          </Form.Item>
          
          <Form.Item
            label="角色"
            name="role"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select placeholder="请选择角色">
              <Option value={1}>管理员</Option>
              <Option value={2}>普通用户</Option>
            </Select>
          </Form.Item>
          
          {!editingUser && (
            <Form.Item
              label="初始余额"
              name="balance"
              initialValue={0}
            >
              <Input type="number" placeholder="请输入初始余额" />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  );
};

export default Users;