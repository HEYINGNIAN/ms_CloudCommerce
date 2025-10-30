import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import { Layout, Menu, Typography } from 'antd';
import Users from './pages/Users';
import Products from './pages/Products';
import Orders from './pages/Orders';
import './App.css';

const { Header, Content, Footer } = Layout;
const { Title } = Typography;
const { SubMenu } = Menu;

function App() {
  return (
    <Router>
      <Layout className="layout">
        <Header className="header">
          <div className="logo">
            <Title level={3} style={{ color: 'white', margin: 0 }}>分布式系统演示</Title>
          </div>
          <Menu
            theme="dark"
            mode="horizontal"
            defaultSelectedKeys={['1']}
            style={{ lineHeight: '64px' }}
          >
            <Menu.Item key="1">
              <Link to="/">首页</Link>
            </Menu.Item>
            <Menu.Item key="2">
              <Link to="/users">用户管理</Link>
            </Menu.Item>
            <Menu.Item key="3">
              <Link to="/products">产品管理</Link>
            </Menu.Item>
            <Menu.Item key="4">
              <Link to="/orders">订单管理</Link>
            </Menu.Item>
          </Menu>
        </Header>
        <Content style={{ padding: '0 50px' }}>
          <div className="site-layout-content">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/users" element={<Users />} />
              <Route path="/products" element={<Products />} />
              <Route path="/orders" element={<Orders />} />
            </Routes>
          </div>
        </Content>
        <Footer style={{ textAlign: 'center' }}>分布式系统演示 ©2024 Created by Java Team</Footer>
      </Layout>
    </Router>
  );
}

// 首页组件
function Home() {
  return (
    <div style={{ padding: '30px', textAlign: 'center' }}>
      <Title level={2}>欢迎使用分布式系统演示平台</Title>
      <p style={{ fontSize: '18px', color: '#666' }}>
        这是一个基于Spring Cloud的分布式系统演示项目，包含用户服务、产品服务、订单服务、API网关和消息消费者等模块。
      </p>
    </div>
  );
}

export default App;