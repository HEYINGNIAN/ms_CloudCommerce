import axios from 'axios';

// 创建axios实例
const api = axios.create({
  baseURL: '/api',
  timeout: 10000
});

// 请求拦截器
api.interceptors.request.use(
  config => {
    return config;
  },
  error => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器 - 处理响应数据中的ID字段，确保它们被当作字符串处理
api.interceptors.response.use(
  response => {
    // 如果响应中有data字段，递归处理其中的ID字段
    if (response.data && typeof response.data === 'object') {
      processIdsInObject(response.data);
    }
    return response;
  },
  error => {
    console.error('响应错误:', error);
    return Promise.reject(error);
  }
);

// 递归处理对象中的ID字段，将数字ID转换为字符串
function processIdsInObject(obj) {
  if (!obj || typeof obj !== 'object') return;
  
  // 处理数组
  if (Array.isArray(obj)) {
    obj.forEach(processIdsInObject);
    return;
  }
  
  // 处理对象
  for (const key in obj) {
    if (obj.hasOwnProperty(key)) {
      // 将ID字段从数字转换为字符串
      if ((key === 'id' || key.endsWith('Id')) && typeof obj[key] === 'number') {
        obj[key] = String(obj[key]);
      } else if (typeof obj[key] === 'object') {
        // 递归处理嵌套对象
        processIdsInObject(obj[key]);
      }
    }
  }
}

export default api;